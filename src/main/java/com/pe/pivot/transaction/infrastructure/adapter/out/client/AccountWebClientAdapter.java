package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TargetProductNotFoundException;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.AccountClientResponse;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.RemoteBalanceUpdateRequest;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.RemoteErrorResponse;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Output Adapter communicating reactively with ms-account (:8082) to execute deposits and withdrawals.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountWebClientAdapter implements AccountClientPort {

  private final WebClient accountWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  /**
   * Updates the account balance for a given account ID by sending a PATCH request to the ms-account service.
   * It handles different HTTP response statuses and maps them to appropriate exceptions or returns the updated ProductBalanceSnapshot.
   *
   * @param accountId - the ID of the account to be updated
   * @param operationType - the type of operation (e.g., DEPOSIT, WITHDRAWAL)
   * @param amount - the amount to be added or subtracted from the account balance
   * @return - a Single emitting the updated ProductBalanceSnapshot or an error if the operation fails
   * @throws TargetProductNotFoundException if the account is not found (HTTP 404)
   */
  @Override
  public Single<ProductBalanceSnapshot> updateAccountBalance(String accountId, String operationType, BigDecimal amount) {
    log.debug("Calling ms-account PATCH /{}/balance with operation: {} and amount: {}", accountId, operationType, amount);

    // Build the request payload for the balance update
    RemoteBalanceUpdateRequest payload = RemoteBalanceUpdateRequest.builder()
            .operationType(operationType)
            .amount(amount)
            .build();

     // Create a Mono that sends the PATCH request to the ms-account service and handles the response
    Mono<ProductBalanceSnapshot> mono = accountWebClient.patch()
            .uri("/{id}/balance", accountId)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> mapClientError(response, accountId))
            .bodyToMono(AccountClientResponse.class)
            .map(dto -> toSnapshot(dto, operationType, amount))
            // Apply a circuit breaker to handle potential failures when calling the ms-account service
            .transform(it -> circuitBreakerFactory.create("accountServiceCircuitBreaker")
                    .run(it, throwable -> {
                      if (throwable instanceof BusinessRuleException || throwable instanceof TargetProductNotFoundException) {
                        return Mono.error(throwable);
                      }

                      // Log the circuit breaker trigger and return a BusinessRuleException indicating that the ms-account service is unavailable or timed out
                      log.error("Circuit breaker triggered calling ms-account for accountId {}: {}",
                              accountId, throwable.getMessage());
                      return Mono.error(new BusinessRuleException("Account service (ms-account) is currently unavailable or timed out (2s)."));
                    }));

    // Convert the Mono to a Single and return it to the caller
    return RxJava3Adapter.monoToSingle(mono);
  }

  /**
   * Maps client-side HTTP errors (4xx) to appropriate exceptions. If the error is a 404, it throws a TargetProductNotFoundException.
   *
   * @param response - the ClientResponse containing the error details
   * @param accountId - the ID of the account for which the operation was attempted
   * @return - a Mono that emits an error with the appropriate exception
   * @throws TargetProductNotFoundException if the account is not found (HTTP 404)
   * @throws BusinessRuleException for other client-side errors (4xx) with the error message from the response body or a default message
   */
  private Mono<? extends Throwable> mapClientError(ClientResponse response, String accountId) {
    if (response.statusCode() == HttpStatus.NOT_FOUND) {
      return Mono.error(new TargetProductNotFoundException("Bank account not found in ms-account with ID: " + accountId));
    }

    // For other 4xx errors, extract the error message
    // from the response body and throw a BusinessRuleException with that message or a default message if none is provided.
    return response.bodyToMono(RemoteErrorResponse.class)
            .map(err -> new BusinessRuleException(
                    Optional.ofNullable(err.message()).orElse("Account operation rejected by ms-account.")))
            .switchIfEmpty(Mono.error(new BusinessRuleException("Account operation rejected by ms-account.")));
  }

  /**
   * Maps an AccountClientResponse to a ProductBalanceSnapshot.
   * This method calculates the previous balance based on the operation type and amount.
   *
   * @param dto - the ClientResponse containing the error details
   * @param operationType - the type of operation being performed (e.g., DEPOSIT, WITHDRAWAL)
   * @param amount - the amount involved in the operation
   * @return - a Mono that emits an error with the appropriate exception
   */
  private ProductBalanceSnapshot toSnapshot(AccountClientResponse dto, String operationType, BigDecimal amount) {
    // Calculate the new balance, commission, and previous balance based on the operation type and amount.
    BigDecimal newBalance = Optional.ofNullable(dto.balance()).orElse(BigDecimal.ZERO);
    BigDecimal commission = Optional.ofNullable(dto.lastAppliedCommission()).orElse(BigDecimal.ZERO);
    BigDecimal previousBalance = "DEPOSIT".equalsIgnoreCase(operationType)
            ? newBalance.subtract(amount).add(commission)
            : newBalance.add(amount).add(commission);

    // Construct and return a ProductBalanceSnapshot with the calculated values and other relevant details from the AccountClientResponse.
    return ProductBalanceSnapshot.builder()
            .productId(dto.id())
            .maskedProductNumber(dto.accountNumber())
            .currency(dto.currency())
            .previousBalance(previousBalance)
            .newBalance(newBalance)
            .appliedCommission(commission)
            .build();
  }
}

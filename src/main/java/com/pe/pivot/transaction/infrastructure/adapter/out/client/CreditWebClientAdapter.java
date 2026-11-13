package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TargetProductNotFoundException;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.CreditClientResponse;
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
 * Output Adapter communicating reactively with ms-credit (:8083) to execute payments and consumptions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditWebClientAdapter implements CreditClientPort {

  private final WebClient creditWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  /**
   * Updates the credit balance for a given credit ID by sending a PATCH request to the ms-credit service.
   * It constructs a payload with the operation type and amount, sends the request, and maps the response to a ProductBalanceSnapshot.
   * If the credit ID is not found or an error occurs, it returns an appropriate error.
   *
   * @param creditId - The unique identifier of the credit account to be updated.
   * @param operationType - The type of operation to be performed (e.g., "payment", "consumption").
   * @param amount - The amount to be added or subtracted from the credit balance.
   * @return - A Single emitting the updated ProductBalanceSnapshot or an error if the operation fails.
   * @throws TargetProductNotFoundException if the credit ID does not exist in ms-credit.
   * @throws BusinessRuleException if the operation is rejected by ms-credit due to business rules
   */
  @Override
  public Single<ProductBalanceSnapshot> updateCreditBalance(String creditId, String operationType, BigDecimal amount) {
    log.debug("Calling ms-credit PATCH /{}/balance with operation: {} and amount: {}",
            creditId, operationType, amount);

    // Build the request payload for the ms-credit service
    RemoteBalanceUpdateRequest payload = RemoteBalanceUpdateRequest.builder()
            .operationType(operationType)
            .amount(amount)
            .build();

    // Send the PATCH request to ms-credit and handle the response reactively
    Mono<ProductBalanceSnapshot> mono = creditWebClient.patch()
            .uri("/{id}/balance", creditId)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> mapClientError(response, creditId))
            .bodyToMono(CreditClientResponse.class)
            .map(dto -> toSnapshot(dto, operationType, amount))

            // Apply a circuit breaker to handle potential failures in the ms-credit service, providing a fallback for non-business rule exceptions
            .transform(it -> circuitBreakerFactory.create("creditServiceCircuitBreaker")
                    .run(it, throwable -> {
                      if (throwable instanceof BusinessRuleException || throwable instanceof TargetProductNotFoundException) {
                        return Mono.error(throwable);
                      }

                      // Log the error and return a BusinessRuleException for circuit breaker fallback
                      log.error("Circuit breaker triggered calling ms-credit for creditId {}: {}", creditId, throwable.getMessage());
                      return Mono.error(new BusinessRuleException("Credit service (ms-credit) is currently unavailable or timed out (2s)."));
                    }));

    return RxJava3Adapter.monoToSingle(mono);
  }

  /**
   * Maps client errors from the ms-credit service to appropriate exceptions.
   * If the response status is 404, it throws a TargetProductNotFoundException.
   *
   * @param response - The ClientResponse received from the ms-credit service.
   * @param creditId - The credit ID that was used in the request, for error context.
   * @return - A Mono that emits an error corresponding to the client error received.
   */
  private Mono<? extends Throwable> mapClientError(ClientResponse response, String creditId) {
    // Log the client error for debugging and monitoring purposes
    if (response.statusCode() == HttpStatus.NOT_FOUND) {
      return Mono.error(new TargetProductNotFoundException("Credit product not found in ms-credit with ID: " + creditId));
    }

    // For other 4xx errors, attempt to extract the error message from the response body and throw a BusinessRuleException with that message.
    return response.bodyToMono(RemoteErrorResponse.class)
            .map(err -> new BusinessRuleException(Optional.ofNullable(err.message()).orElse("Credit operation rejected by ms-credit.")))
            .switchIfEmpty(Mono.error(new BusinessRuleException("Credit operation rejected by ms-credit.")));
  }

  /**
   * Converts the CreditClientResponse DTO to a ProductBalanceSnapshot,
   * calculating the previous and new balances based on the operation type and amount.
   *
   * @param dto - The response DTO received from ms-credit containing the updated credit information.
   * @param operationType - The type of operation performed (e.g., "payment", "consumption").
   * @param amount - The amount that was added or subtracted from the credit balance.
   * @return - A ProductBalanceSnapshot representing the previous and new balances after the operation.
   */
  private ProductBalanceSnapshot toSnapshot(CreditClientResponse dto, String operationType, BigDecimal amount) {

    // Calculate the new and previous debt balances based on the operation type and amount.
    BigDecimal newDebtBalance = Optional.ofNullable(dto.usedAmount()).orElse(BigDecimal.ZERO);
    BigDecimal previousDebtBalance = "CONSUMPTION".equalsIgnoreCase(operationType)
            ? newDebtBalance.subtract(amount)
            : newDebtBalance.add(amount);

    // Construct and return a ProductBalanceSnapshot with the relevant details,
    // including product ID, masked product number, currency, previous balance, new balance, and applied commission.
    return ProductBalanceSnapshot.builder()
            .productId(dto.id())
            .maskedProductNumber(dto.creditNumber())
            .currency(dto.currency())
            .previousBalance(previousDebtBalance)
            .newBalance(newDebtBalance)
            .appliedCommission(BigDecimal.ZERO)
            .build();
  }
}

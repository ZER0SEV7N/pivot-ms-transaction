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

    RemoteBalanceUpdateRequest payload = RemoteBalanceUpdateRequest.builder()
            .operationType(operationType)
            .amount(amount)
            .build();
    Mono<ProductBalanceSnapshot> mono = accountWebClient.patch()
            .uri("/{id}/balance", accountId)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> mapClientError(response, accountId))
            .bodyToMono(AccountClientResponse.class)
            .map(dto -> toSnapshot(dto, operationType, amount));

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
      return Mono.error(new TargetProductNotFoundException(
              "Bank account not found in ms-account with ID: " + accountId));
    }
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
    BigDecimal newBalance = Optional.ofNullable(dto.balance()).orElse(BigDecimal.ZERO);
    BigDecimal previousBalance = "DEPOSIT".equalsIgnoreCase(operationType)
            ? newBalance.subtract(amount)
            : newBalance.add(amount);

    return ProductBalanceSnapshot.builder()
            .productId(dto.id())
            .maskedProductNumber(dto.accountNumber())
            .currency(dto.currency())
            .previousBalance(previousBalance)
            .newBalance(newBalance)
            .build();
  }
}

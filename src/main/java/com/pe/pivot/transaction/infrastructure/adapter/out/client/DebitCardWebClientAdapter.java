package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TargetProductNotFoundException;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.port.out.DebitCardClientPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.DebitCardPaymentClientResponse;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.RemoteDebitCardPaymentRequest;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.RemoteErrorResponse;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.Optional;
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

/**
 * Output Adapter communicating reactively with ms-account (:8082) for debit card payments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebitCardWebClientAdapter implements DebitCardClientPort {

  private static final String CIRCUIT_BREAKER_NAME = "accountServiceCircuitBreaker";
  private static final String DEFAULT_ERROR = "Debit card operation rejected by ms-account.";

  private final WebClient debitCardWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  @Override
  public Single<ProductBalanceSnapshot> payWithDebitCard(String debitCardId, BigDecimal amount) {
    log.debug("Calling ms-account PATCH /debit-cards/{}/pay with amount: {}", debitCardId, amount);

    RemoteDebitCardPaymentRequest payload = RemoteDebitCardPaymentRequest.builder()
            .amount(amount)
            .build();

    Mono<ProductBalanceSnapshot> mono = debitCardWebClient.patch()
            .uri("/{id}/pay", debitCardId)
            .bodyValue(payload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                    response -> mapClientError(response, debitCardId))
            .bodyToMono(DebitCardPaymentClientResponse.class)
            .map(this::toSnapshot)
            .transform(it -> circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME)
                    .run(it, throwable -> {
                      if (throwable instanceof BusinessRuleException
                              || throwable instanceof TargetProductNotFoundException) {
                        return Mono.error(throwable);
                      }
                      log.error(
                              "Circuit breaker triggered calling ms-account debit-cards for {}: {}",
                              debitCardId, throwable.getMessage());
                      return Mono.error(new BusinessRuleException(
                              "Account service (ms-account) is currently unavailable or timed out "
                                      + "(2s)."));
                    }));

    return RxJava3Adapter.monoToSingle(mono);
  }

  private Mono<? extends Throwable> mapClientError(ClientResponse response, String debitCardId) {
    if (response.statusCode() == HttpStatus.NOT_FOUND) {
      return Mono.error(new TargetProductNotFoundException(
              "Debit card not found in ms-account with ID: " + debitCardId));
    }
    return response.bodyToMono(RemoteErrorResponse.class)
            .map(err -> new BusinessRuleException(
                    Optional.ofNullable(err.message()).orElse(DEFAULT_ERROR)))
            .switchIfEmpty(Mono.error(new BusinessRuleException(DEFAULT_ERROR)));
  }

  private ProductBalanceSnapshot toSnapshot(DebitCardPaymentClientResponse dto) {
    BigDecimal previousBalance = dto.debitedAccounts() != null && !dto.debitedAccounts().isEmpty()
            ? dto.debitedAccounts().get(0).previousBalance()
            : BigDecimal.ZERO;
    BigDecimal newBalance = dto.debitedAccounts() != null && !dto.debitedAccounts().isEmpty()
            ? dto.debitedAccounts().get(0).newBalance()
            : BigDecimal.ZERO;

    return ProductBalanceSnapshot.builder()
            .productId(dto.debitCardId())
            .maskedProductNumber(dto.maskedCardNumber())
            .currency("PEN")
            .previousBalance(previousBalance)
            .newBalance(newBalance)
            .appliedCommission(Optional.ofNullable(dto.totalCommission()).orElse(BigDecimal.ZERO))
            .build();
  }
}
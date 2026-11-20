package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TargetProductNotFoundException;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.support.TestCircuitBreakerFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link DebitCardWebClientAdapter}.
 */
@DisplayName("Should be able to pay with a debit card through ms-account")
class DebitCardWebClientAdapterTest {

  private static final String BASE_URL = "http://localhost/api/v1/debit-cards";

  @Test
  @DisplayName("Should be mapped to a balance snapshot when the payment succeeds")
  void shouldBeMappedToBalanceSnapshotWhenPaymentSucceeds() {
    // (happy path)
    // Arrange
    ExchangeFunction exchange = request -> {
      assertThat(request.method()).isEqualTo(HttpMethod.PATCH);
      assertThat(request.url().getPath()).endsWith("/dc-1/pay");
      return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"debitCardId\":\"dc-1\",\"maskedCardNumber\":\"****1234\","
                      + "\"totalAmount\":100.00,\"totalCommission\":1.50,"
                      + "\"debitedAccounts\":[{\"accountId\":\"acc-1\","
                      + "\"maskedAccountNumber\":\"****5678\",\"debitedAmount\":100.00,"
                      + "\"appliedCommission\":1.50,\"previousBalance\":500.00,"
                      + "\"newBalance\":400.00}],\"processedAt\":\"2026-01-15T10:00:00Z\"}")
              .build());
    };
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getProductId()).isEqualTo("dc-1");
    assertThat(snapshot.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(snapshot.getCurrency()).isEqualTo("PEN");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("500.00");
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("400.00");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("1.50");
  }

  @Test
  @DisplayName("Should be defaulted to zero when no account was debited")
  void shouldBeDefaultedToZeroWhenNoAccountWasDebited() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"debitCardId\":\"dc-1\",\"maskedCardNumber\":\"****1234\","
                    + "\"debitedAccounts\":[]}")
            .build());
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should be defaulted to zero when the debited accounts field is absent")
  void shouldBeDefaultedToZeroWhenDebitedAccountsFieldIsAbsent() {
    // (error: incomplete downstream response)
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"debitCardId\":\"dc-1\",\"maskedCardNumber\":\"****1234\"}")
            .build());
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should be translated to a not found error when ms-account returns 404")
  void shouldBeTranslatedToNotFoundErrorWhenMsAccountReturns404() {
    // (error: debit card not found)
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .test()
            .assertError(TargetProductNotFoundException.class)
            .assertError(hasErrorMessage(
                    "Debit card not found in ms-account with ID: dc-1"));
  }

  @Test
  @DisplayName("Should be translated to a business rule error when ms-account returns a message")
  void shouldBeTranslatedToBusinessRuleErrorWhenMsAccountReturnsMessage() {
    // (error: rejected payment with message)
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse
            .create(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"message\":\"Insufficient funds\"}")
            .build());
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Insufficient funds"));
  }

  @Test
  @DisplayName("Should be translated to the default error when ms-account returns no body")
  void shouldBeTranslatedToDefaultErrorWhenMsAccountReturnsNoBody() {
    // (error: rejected payment without message)
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Debit card operation rejected by ms-account."));
  }

  @Test
  @DisplayName("Should be propagated when the downstream returns a business error")
  void shouldBePropagatedWhenDownstreamReturnsBusinessError() {
    // (error: business rule already mapped)
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.error(new BusinessRuleException("Cascading debit failed"));
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Cascading debit failed"));
  }

  @Test
  @DisplayName("Should be translated to an unavailable service error on unexpected failures")
  void shouldBeTranslatedToUnavailableServiceErrorOnUnexpectedFailures() {
    // (error: connection failure)
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.error(new IllegalStateException("Connection reset"));
    DebitCardWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.payWithDebitCard("dc-1", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Account service (ms-account) is currently unavailable or timed out (2s)."));
  }

  private DebitCardWebClientAdapter adapterWith(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .exchangeFunction(exchangeFunction)
            .build();
    return new DebitCardWebClientAdapter(webClient, TestCircuitBreakerFactory.passThrough());
  }
}

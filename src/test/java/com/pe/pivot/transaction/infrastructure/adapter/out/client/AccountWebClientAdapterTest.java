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

class AccountWebClientAdapterTest {

  private static final String BASE_URL = "http://localhost/api/v1/accounts";

  @Test
  @DisplayName("Should map a deposit response and compute the previous balance")
  void shouldMapDepositResponse() {
    // Arrange
    ExchangeFunction exchange = request -> {
      assertThat(request.method()).isEqualTo(HttpMethod.PATCH);
      assertThat(request.url().getPath()).endsWith("/acc-1/balance");
      return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"id\":\"acc-1\",\"accountNumber\":\"****1234\",\"currency\":\"PEN\","
                      + "\"balance\":600.00,\"lastAppliedCommission\":1.00}")
              .build());
    };
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateAccountBalance("acc-1", "DEPOSIT", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getProductId()).isEqualTo("acc-1");
    assertThat(snapshot.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(snapshot.getCurrency()).isEqualTo("PEN");
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("600.00");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("1.00");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("501.00");
  }

  @Test
  @DisplayName("Should map a withdrawal response and compute the previous balance")
  void shouldMapWithdrawalResponse() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"id\":\"acc-1\",\"accountNumber\":\"****1234\",\"currency\":\"PEN\","
                    + "\"balance\":400.00,\"lastAppliedCommission\":0.00}")
            .build());
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateAccountBalance("acc-1", "WITHDRAWAL", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("400.00");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("500.00");
  }

  @Test
  @DisplayName("Should default missing balance and commission values to zero")
  void shouldDefaultMissingValuesToZero() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"id\":\"acc-1\",\"accountNumber\":\"****1234\",\"currency\":\"PEN\"}")
            .build());
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateAccountBalance("acc-1", "WITHDRAWAL", new BigDecimal("100.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("0");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Should translate a 404 into a target product not found error")
  void shouldTranslateNotFound() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateAccountBalance("acc-1", "DEPOSIT", new BigDecimal("100.00"))
            .test()
            .assertError(TargetProductNotFoundException.class)
            .assertError(hasErrorMessage("Bank account not found in ms-account with ID: acc-1"));
  }

  @Test
  @DisplayName("Should translate a 4xx with a body into a business rule error")
  void shouldTranslateClientErrorWithMessage() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"message\":\"Insufficient funds\"}")
            .build());
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateAccountBalance("acc-1", "WITHDRAWAL", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Insufficient funds"));
  }

  @Test
  @DisplayName("Should translate a 4xx without a body into the default business rule error")
  void shouldTranslateClientErrorWithoutMessage() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateAccountBalance("acc-1", "WITHDRAWAL", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Account operation rejected by ms-account."));
  }

  @Test
  @DisplayName("Should translate unexpected failures into a service unavailable error")
  void shouldTranslateUnexpectedFailure() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.error(new IllegalStateException("Connection reset"));
    AccountWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateAccountBalance("acc-1", "DEPOSIT", new BigDecimal("100.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Account service (ms-account) is currently unavailable or timed out (2s)."));
  }

  private AccountWebClientAdapter adapterWith(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .exchangeFunction(exchangeFunction)
            .build();
    return new AccountWebClientAdapter(webClient, TestCircuitBreakerFactory.passThrough());
  }
}

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

class CreditWebClientAdapterTest {

  private static final String BASE_URL = "http://localhost/api/v1/credits";

  @Test
  @DisplayName("Should map a consumption response and compute the previous debt balance")
  void shouldMapConsumptionResponse() {
    // Arrange
    ExchangeFunction exchange = request -> {
      assertThat(request.method()).isEqualTo(HttpMethod.PATCH);
      assertThat(request.url().getPath()).endsWith("/cred-1/balance");
      return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"id\":\"cred-1\",\"creditNumber\":\"****5678\",\"currency\":\"PEN\","
                      + "\"usedAmount\":250.00}")
              .build());
    };
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateCreditBalance("cred-1", "CONSUMPTION", new BigDecimal("50.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getProductId()).isEqualTo("cred-1");
    assertThat(snapshot.getMaskedProductNumber()).isEqualTo("****5678");
    assertThat(snapshot.getCurrency()).isEqualTo("PEN");
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("250.00");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("200.00");
    assertThat(snapshot.getAppliedCommission()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Should map a payment response and compute the previous debt balance")
  void shouldMapPaymentResponse() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"id\":\"cred-1\",\"creditNumber\":\"****5678\",\"currency\":\"PEN\","
                    + "\"usedAmount\":200.00}")
            .build());
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateCreditBalance("cred-1", "PAYMENT", new BigDecimal("50.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("200.00");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("250.00");
  }

  @Test
  @DisplayName("Should default a missing used amount to zero")
  void shouldDefaultMissingUsedAmount() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"id\":\"cred-1\",\"creditNumber\":\"****5678\",\"currency\":\"PEN\"}")
            .build());
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act
    ProductBalanceSnapshot snapshot = adapter
            .updateCreditBalance("cred-1", "PAYMENT", new BigDecimal("50.00"))
            .blockingGet();

    // Assert
    assertThat(snapshot.getNewBalance()).isEqualByComparingTo("0");
    assertThat(snapshot.getPreviousBalance()).isEqualByComparingTo("50.00");
  }

  @Test
  @DisplayName("Should translate a 404 into a target product not found error")
  void shouldTranslateNotFound() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateCreditBalance("cred-1", "PAYMENT", new BigDecimal("50.00"))
            .test()
            .assertError(TargetProductNotFoundException.class)
            .assertError(hasErrorMessage("Credit product not found in ms-credit with ID: cred-1"));
  }

  @Test
  @DisplayName("Should translate a 4xx with a body into a business rule error")
  void shouldTranslateClientErrorWithMessage() {
    // Arrange
    ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body("{\"message\":\"Credit limit exceeded\"}")
            .build());
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateCreditBalance("cred-1", "CONSUMPTION", new BigDecimal("50.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Credit limit exceeded"));
  }

  @Test
  @DisplayName("Should translate a 4xx without a body into the default business rule error")
  void shouldTranslateClientErrorWithoutMessage() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateCreditBalance("cred-1", "CONSUMPTION", new BigDecimal("50.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Credit operation rejected by ms-credit."));
  }

  @Test
  @DisplayName("Should translate unexpected failures into a service unavailable error")
  void shouldTranslateUnexpectedFailure() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.error(new IllegalStateException("Connection reset"));
    CreditWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.updateCreditBalance("cred-1", "PAYMENT", new BigDecimal("50.00"))
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Credit service (ms-credit) is currently unavailable or timed out (2s)."));
  }

  private CreditWebClientAdapter adapterWith(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .exchangeFunction(exchangeFunction)
            .build();
    return new CreditWebClientAdapter(webClient, TestCircuitBreakerFactory.passThrough());
  }
}

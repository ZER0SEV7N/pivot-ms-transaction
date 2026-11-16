package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.CustomerSummary;
import com.pe.pivot.transaction.support.TestCircuitBreakerFactory;
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

class CustomerWebClientAdapterTest {

  private static final String BASE_URL = "http://localhost/api/v1/customers";

  @Test
  @DisplayName("Should map a customer response into the domain summary")
  void shouldMapCustomerResponse() {
    // Arrange
    ExchangeFunction exchange = request -> {
      assertThat(request.method()).isEqualTo(HttpMethod.GET);
      assertThat(request.url().getPath()).endsWith("/cust-1");
      return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"id\":\"cust-1\",\"customerType\":\"NATURAL\",\"profile\":\"RETAIL\","
                      + "\"documentType\":\"DNI\",\"documentNumber\":\"12345678\","
                      + "\"status\":\"ACTIVE\"}")
              .build());
    };
    CustomerWebClientAdapter adapter = adapterWith(exchange);

    // Act
    CustomerSummary summary = adapter.findCustomerById("cust-1").blockingGet();

    // Assert
    assertThat(summary.getId()).isEqualTo("cust-1");
    assertThat(summary.getCustomerType()).isEqualTo("NATURAL");
    assertThat(summary.getDocumentNumber()).isEqualTo("12345678");
    assertThat(summary.getStatus()).isEqualTo("ACTIVE");
    assertThat(summary.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should complete empty when the customer is not found")
  void shouldCompleteEmptyWhenNotFound() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
    CustomerWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.findCustomerById("missing")
            .test()
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should translate unexpected failures into a service unavailable error")
  void shouldTranslateUnexpectedFailure() {
    // Arrange
    ExchangeFunction exchange = request ->
            Mono.error(new IllegalStateException("Connection reset"));
    CustomerWebClientAdapter adapter = adapterWith(exchange);

    // Act & Assert
    adapter.findCustomerById("cust-1")
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Customer service (ms-customer) is currently unavailable or timed out (2s)."));
  }

  private CustomerWebClientAdapter adapterWith(ExchangeFunction exchangeFunction) {
    WebClient webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .exchangeFunction(exchangeFunction)
            .build();
    return new CustomerWebClientAdapter(webClient, TestCircuitBreakerFactory.passThrough());
  }
}

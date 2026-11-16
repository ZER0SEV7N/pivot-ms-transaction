package com.pe.pivot.transaction.infrastructure.adapter.in.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

class GlobalExceptionHandlerTest {

  private static final String PATH = "/api/v1/transactions/tx-1";

  private GlobalExceptionHandler handler;
  private ServerWebExchange exchange;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    exchange = exchangeWithPath(PATH);
  }

  @Test
  @DisplayName("Should map a not found exception into a 404 response")
  void shouldMapNotFound() {
    // Arrange
    TransactionNotFoundException exception =
            new TransactionNotFoundException("Transaction not found with ID: tx-1");

    // Act
    TestObserver<ErrorResponse> observer = handler.handleNotFound(exception, exchange).test();

    // Assert
    observer.assertComplete().assertNoErrors();
    observer.assertValue(response -> matches(
            response, HttpStatus.NOT_FOUND, "Transaction not found with ID: tx-1"));
  }

  @Test
  @DisplayName("Should map a business rule exception into a 400 response")
  void shouldMapBusinessRule() {
    // Arrange
    BusinessRuleException exception = new BusinessRuleException("Insufficient funds");

    // Act
    TestObserver<ErrorResponse> observer = handler.handleBusinessRule(exception, exchange).test();

    // Assert
    observer.assertValue(response ->
            matches(response, HttpStatus.BAD_REQUEST, "Insufficient funds")).assertComplete();
  }

  @Test
  @DisplayName("Should join every validation error into a 400 response")
  void shouldMapValidationErrors() {
    // Arrange
    WebExchangeBindException exception = mock(WebExchangeBindException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getAllErrors()).thenReturn(List.of(
            new ObjectError("request", "customerId is required"),
            new ObjectError("request", "amount must be greater than zero")));

    // Act
    TestObserver<ErrorResponse> observer =
            handler.handleValidationErrors(exception, exchange).test();

    // Assert
    observer.assertValue(response -> matches(response, HttpStatus.BAD_REQUEST,
            "customerId is required; amount must be greater than zero")).assertComplete();
  }

  @Test
  @DisplayName("Should map an unexpected exception into a 500 response")
  void shouldMapGenericException() {
    // Arrange
    Exception exception = new IllegalStateException("Boom");

    // Act
    TestObserver<ErrorResponse> observer =
            handler.handleGenericException(exception, exchange).test();

    // Assert
    observer.assertValue(response -> matches(response, HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal server error occurred.")).assertComplete();
  }

  private boolean matches(ErrorResponse response, HttpStatus status, String message) {
    assertThat(response.status()).isEqualTo(status.value());
    assertThat(response.error()).isEqualTo(status.getReasonPhrase());
    assertThat(response.message()).isEqualTo(message);
    assertThat(response.path()).isEqualTo(PATH);
    assertThat(response.timestamp()).isNotNull();
    return true;
  }

  private ServerWebExchange exchangeWithPath(String path) {
    ServerWebExchange webExchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    RequestPath requestPath = mock(RequestPath.class);
    when(requestPath.value()).thenReturn(path);
    when(request.getPath()).thenReturn(requestPath);
    when(webExchange.getRequest()).thenReturn(request);
    return webExchange;
  }
}

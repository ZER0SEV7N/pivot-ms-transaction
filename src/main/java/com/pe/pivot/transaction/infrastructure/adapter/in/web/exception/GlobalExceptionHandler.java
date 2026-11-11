package com.pe.pivot.transaction.infrastructure.adapter.in.web.exception;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.CustomerNotFoundException;
import com.pe.pivot.transaction.domain.exception.TargetProductNotFoundException;
import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global Exception Handler translating domain and validation exceptions into clean OpenAPI ErrorResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
          TransactionNotFoundException.class,
          CustomerNotFoundException.class,
          TargetProductNotFoundException.class
  })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Single<ErrorResponse> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
    log.warn("Resource not found: {}", ex.getMessage());
    return Single.just(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange));
  }

  @ExceptionHandler(BusinessRuleException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Single<ErrorResponse> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
    log.warn("Business rule violation: {}", ex.getMessage());
    return Single.just(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Single<ErrorResponse> handleValidationErrors(WebExchangeBindException ex, ServerWebExchange exchange) {
    String errors = ex.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining("; "));
    log.warn("Payload validation error: {}", errors);
    return Single.just(buildErrorResponse(HttpStatus.BAD_REQUEST, errors, exchange));
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Single<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
    log.error("Unexpected error occurred: ", ex);
    return Single.just(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal server error occurred.", exchange));
  }

  private ErrorResponse buildErrorResponse(HttpStatus status, String message, ServerWebExchange exchange) {
    return ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(exchange.getRequest().getPath().value())
            .build();
  }
}
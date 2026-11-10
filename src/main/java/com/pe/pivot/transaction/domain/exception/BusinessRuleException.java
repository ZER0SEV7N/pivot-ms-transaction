package com.pe.pivot.transaction.domain.exception;

/**
 * Exception thrown when a business rule is violated in the transaction domain.
 */
public class BusinessRuleException extends RuntimeException {
  public BusinessRuleException(String message) {
    super(message);
  }
}

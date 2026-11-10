package com.pe.pivot.transaction.domain.exception;

/**
 * Exception thrown when a transaction is not found in the ledger.
 */
public class CustomerNotFoundException extends RuntimeException {
  public CustomerNotFoundException(String message) {
    super(message);
  }
}

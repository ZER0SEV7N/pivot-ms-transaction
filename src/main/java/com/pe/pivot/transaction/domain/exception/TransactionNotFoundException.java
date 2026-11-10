package com.pe.pivot.transaction.domain.exception;

/**
 * Exception thrown when a transaction is not found in the ledger.
 */
public class TransactionNotFoundException extends RuntimeException {
  public TransactionNotFoundException(String message) {
    super(message);
  }
}

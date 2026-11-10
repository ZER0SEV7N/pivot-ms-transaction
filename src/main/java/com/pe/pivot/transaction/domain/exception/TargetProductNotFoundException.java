package com.pe.pivot.transaction.domain.exception;

/**
 * Exception thrown when a target product is not found in the ledger.
 */
public class TargetProductNotFoundException extends RuntimeException {
  public TargetProductNotFoundException(String message) {
    super(message);
  }
}

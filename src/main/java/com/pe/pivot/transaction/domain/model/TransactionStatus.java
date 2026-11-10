package com.pe.pivot.transaction.domain.model;

/**
 * Lifecycle states of an immutable ledger transaction.
 */
public enum TransactionStatus {
  COMPLETED,
  REVERSED,
  REJECTED
}
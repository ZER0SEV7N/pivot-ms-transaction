package com.pe.pivot.transaction.domain.strategy;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import io.reactivex.rxjava3.core.Single;

/**
 * Strategy interface for executing a specific financial transaction type.
 */
public interface TransactionExecutionStrategy {

  /**
   * Returns the transaction type that this strategy supports.
   *
   * @return - the supported transaction type:
   *  - DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REVERSAL, etc.
   */
  TransactionType getSupportedType();

  /**
   * Executes the transaction according to the specific business rules of the transaction type.
   *
   * @param transaction - the transaction to be executed
   * @param operationPrefix - the prefix to be used for generating the operation number
   * @param defaultFee - the default fee to be applied if applicable
   * @return - a Single emitting the executed transaction with updated details (e.g., operation number, status, etc.)
   */
  Single<Transaction> execute(Transaction transaction, String operationPrefix, java.math.BigDecimal defaultFee);
}
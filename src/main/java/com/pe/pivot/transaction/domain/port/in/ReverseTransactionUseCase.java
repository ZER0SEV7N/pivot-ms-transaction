package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;

/**
 * Input port for executing compensating transaction reversals.
 */
public interface ReverseTransactionUseCase {

  Single<Transaction> reverse(String transactionId, String reason);
}

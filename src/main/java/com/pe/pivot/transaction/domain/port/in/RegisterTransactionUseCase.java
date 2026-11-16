package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;

/**
 * Input port for registering and executing a new financial transaction.
 */
public interface RegisterTransactionUseCase {

  Single<Transaction> register(Transaction transaction);
}

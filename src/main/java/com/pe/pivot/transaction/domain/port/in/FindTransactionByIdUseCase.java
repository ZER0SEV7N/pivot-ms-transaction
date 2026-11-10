package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Single;

public interface FindTransactionByIdUseCase {
  Single<Transaction> findById(String id);
}
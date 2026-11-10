package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface RetrieveTransactionUseCase {
  Flowable<Transaction> findAll();
  Single<Transaction> findById(String id);
  Single<Transaction> findByOperationNumber(String operationNumber);
  Flowable<Transaction> findByCustomerId(String customerId);
  Flowable<Transaction> findByTargetProductId(String targetProductId);
}
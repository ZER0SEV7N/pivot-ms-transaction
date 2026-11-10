package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface TransactionRepositoryPort {
  Single<Transaction> save(Transaction transaction);
  Flowable<Transaction> findAll();
  Maybe<Transaction> findById(String id);
  Maybe<Transaction> findByOperationNumber(String operationNumber);
  Flowable<Transaction> findByCustomerId(String customerId);
  Flowable<Transaction> findByTargetProductId(String targetProductId);
}
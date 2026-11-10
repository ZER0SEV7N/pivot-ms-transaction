package com.pe.pivot.transaction.infrastructure.adapter.out.persistence;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.repository.ReactiveTransactionMongoRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * Persistence Adapter bridging Spring Data Reactive MongoDB (Reactor) to RxJava 3 Domain Ports.
 */
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

  private final ReactiveTransactionMongoRepository repository;
  private final TransactionPersistenceMapper mapper;

  @Override
  public Single<Transaction> save(Transaction transaction) {
    return RxJava3Adapter.monoToSingle(
            repository.save(mapper.toDocument(transaction))
                    .map(mapper::toDomain)
    );
  }

  @Override
  public Flowable<Transaction> findAll() {
    return RxJava3Adapter.fluxToFlowable(
            repository.findAll()
                    .map(mapper::toDomain)
    );
  }

  @Override
  public Maybe<Transaction> findById(String id) {
    return RxJava3Adapter.monoToMaybe(
            repository.findById(id)
                    .map(mapper::toDomain)
    );
  }

  @Override
  public Maybe<Transaction> findByOperationNumber(String operationNumber) {
    return RxJava3Adapter.monoToMaybe(
            repository.findByOperationNumber(operationNumber)
                    .map(mapper::toDomain)
    );
  }

  @Override
  public Flowable<Transaction> findByCustomerId(String customerId) {
    return RxJava3Adapter.fluxToFlowable(
            repository.findByCustomerId(customerId)
                    .map(mapper::toDomain)
    );
  }

  @Override
  public Flowable<Transaction> findByTargetProductId(String targetProductId) {
    return RxJava3Adapter.fluxToFlowable(
            repository.findByTargetProductId(targetProductId)
                    .map(mapper::toDomain)
    );
  }
}
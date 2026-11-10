package com.pe.pivot.transaction.infrastructure.adapter.out.persistence.repository;

import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.entity.TransactionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data Reactive MongoDB repository using strictly derived query methods (zero @Query).
 */
@Repository
public interface ReactiveTransactionMongoRepository extends ReactiveMongoRepository<TransactionDocument, String> {

  Mono<TransactionDocument> findByOperationNumber(String operationNumber);

  Flux<TransactionDocument> findByCustomerId(String customerId);

  Flux<TransactionDocument> findByTargetProductId(String targetProductId);
}

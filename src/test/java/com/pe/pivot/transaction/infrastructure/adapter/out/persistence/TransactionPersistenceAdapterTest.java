package com.pe.pivot.transaction.infrastructure.adapter.out.persistence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.entity.TransactionDocument;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.repository.ReactiveTransactionMongoRepository;
import com.pe.pivot.transaction.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterTest {

  @Mock
  private ReactiveTransactionMongoRepository repository;

  @Mock
  private TransactionPersistenceMapper mapper;

  private TransactionPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new TransactionPersistenceAdapter(repository, mapper);
  }

  @Test
  @DisplayName("Should save a transaction through the Mongo repository")
  void shouldSaveTransaction() {
    // Arrange
    Transaction domain = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    TransactionDocument document = new TransactionDocument();
    when(mapper.toDocument(domain)).thenReturn(document);
    when(repository.save(document)).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act & Assert
    adapter.save(domain)
            .test()
            .assertValue(domain)
            .assertComplete()
            .assertNoErrors();
    verify(repository).save(document);
  }

  @Test
  @DisplayName("Should emit every transaction when finding all")
  void shouldFindAllTransactions() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction second = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    TransactionDocument firstDocument = new TransactionDocument();
    TransactionDocument secondDocument = new TransactionDocument();
    when(repository.findAll()).thenReturn(Flux.just(firstDocument, secondDocument));
    when(mapper.toDomain(firstDocument)).thenReturn(first);
    when(mapper.toDomain(secondDocument)).thenReturn(second);

    // Act & Assert
    adapter.findAll()
            .test()
            .assertValues(first, second)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should find a transaction by ID when it exists")
  void shouldFindByIdWhenPresent() {
    // Arrange
    Transaction domain = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    TransactionDocument document = new TransactionDocument();
    when(repository.findById("tx-1")).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act & Assert
    adapter.findById("tx-1")
            .test()
            .assertValue(domain)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should complete empty when the ID does not exist")
  void shouldCompleteEmptyWhenIdMissing() {
    // Arrange
    when(repository.findById("missing")).thenReturn(Mono.empty());

    // Act & Assert
    adapter.findById("missing")
            .test()
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should find a transaction by operation number when it exists")
  void shouldFindByOperationNumberWhenPresent() {
    // Arrange
    Transaction domain = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    TransactionDocument document = new TransactionDocument();
    when(repository.findByOperationNumber("OP-1")).thenReturn(Mono.just(document));
    when(mapper.toDomain(document)).thenReturn(domain);

    // Act & Assert
    adapter.findByOperationNumber("OP-1")
            .test()
            .assertValue(domain)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should complete empty when the operation number does not exist")
  void shouldCompleteEmptyWhenOperationNumberMissing() {
    // Arrange
    when(repository.findByOperationNumber("missing")).thenReturn(Mono.empty());

    // Act & Assert
    adapter.findByOperationNumber("missing")
            .test()
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should find transactions by customer ID")
  void shouldFindByCustomerId() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    TransactionDocument document = new TransactionDocument();
    when(repository.findByCustomerId("cust-1")).thenReturn(Flux.just(document));
    when(mapper.toDomain(document)).thenReturn(first);

    // Act & Assert
    adapter.findByCustomerId("cust-1")
            .test()
            .assertValue(first)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should find transactions by target product ID")
  void shouldFindByTargetProductId() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    TransactionDocument document = new TransactionDocument();
    when(repository.findByTargetProductId("prod-1")).thenReturn(Flux.just(document));
    when(mapper.toDomain(document)).thenReturn(first);

    // Act & Assert
    adapter.findByTargetProductId("prod-1")
            .test()
            .assertValue(first)
            .assertComplete()
            .assertNoErrors();
  }
}

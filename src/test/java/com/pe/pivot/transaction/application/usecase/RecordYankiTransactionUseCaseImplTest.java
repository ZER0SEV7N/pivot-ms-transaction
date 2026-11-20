package com.pe.pivot.transaction.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.application.support.yanki.YankiTransactionFactory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RecordYankiTransactionUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to record Yanki transactions idempotently")
class RecordYankiTransactionUseCaseImplTest {

  private static final String TRANSACTION_ID = "tx-1";
  private static final String OPERATION_NUMBER = "YK-tx-1";

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private YankiTransactionFactory yankiTransactionFactory;

  private RecordYankiTransactionUseCaseImpl useCase;

  private YankiLedgerCommand command;

  @BeforeEach
  void setUp() {
    useCase = new RecordYankiTransactionUseCaseImpl(transactionRepository,
            yankiTransactionFactory);
    command = YankiLedgerCommand.builder()
            .transactionId(TRANSACTION_ID)
            .status("COMPLETED")
            .amount(new BigDecimal("30.00"))
            .build();
  }

  @Test
  @DisplayName("Should be recorded in the ledger when it was not recorded before")
  void shouldBeRecordedInLedgerWhenItWasNotRecordedBefore() {
    // (happy path)
    // Arrange
    Transaction ledgerEntry = Transaction.builder()
            .operationNumber(OPERATION_NUMBER)
            .amount(new BigDecimal("30.00"))
            .build();
    when(yankiTransactionFactory.buildOperationNumber(TRANSACTION_ID))
            .thenReturn(OPERATION_NUMBER);
    when(transactionRepository.findByOperationNumber(OPERATION_NUMBER))
            .thenReturn(Maybe.empty());
    when(yankiTransactionFactory.buildLedgerEntry(command)).thenReturn(ledgerEntry);
    when(transactionRepository.save(ledgerEntry)).thenReturn(Single.just(ledgerEntry));

    // Act
    TestObserver<Void> observer = useCase.recordCompletedPayment(command).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(yankiTransactionFactory).buildLedgerEntry(command);
    verify(transactionRepository).save(ledgerEntry);
  }

  @Test
  @DisplayName("Should be skipped when the Yanki event is not completed")
  void shouldBeSkippedWhenYankiEventIsNotCompleted() {
    // (error: non-completed event)
    // Arrange
    YankiLedgerCommand failed = YankiLedgerCommand.builder()
            .transactionId(TRANSACTION_ID)
            .status("FAILED")
            .build();

    // Act
    TestObserver<Void> observer = useCase.recordCompletedPayment(failed).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verifyNoInteractions(transactionRepository);
    verifyNoInteractions(yankiTransactionFactory);
  }

  @Test
  @DisplayName("Should not be duplicated when the operation number already exists")
  void shouldNotBeDuplicatedWhenOperationNumberAlreadyExists() {
    // Arrange
    Transaction existing = Transaction.builder().operationNumber(OPERATION_NUMBER).build();
    when(yankiTransactionFactory.buildOperationNumber(TRANSACTION_ID))
            .thenReturn(OPERATION_NUMBER);
    when(transactionRepository.findByOperationNumber(OPERATION_NUMBER))
            .thenReturn(Maybe.just(existing));

    // Act
    TestObserver<Void> observer = useCase.recordCompletedPayment(command).test();

    // Assert
    observer.assertNoErrors();
    observer.assertComplete();
    verify(transactionRepository, never()).save(any());
    verify(yankiTransactionFactory, never()).buildLedgerEntry(any());
  }

  @Test
  @DisplayName("Should be propagated when the idempotency lookup fails")
  void shouldBePropagatedWhenIdempotencyLookupFails() {
    // (error: repository lookup failure)
    // Arrange
    when(yankiTransactionFactory.buildOperationNumber(TRANSACTION_ID))
            .thenReturn(OPERATION_NUMBER);
    when(transactionRepository.findByOperationNumber(OPERATION_NUMBER))
            .thenReturn(Maybe.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<Void> observer = useCase.recordCompletedPayment(command).test();

    // Assert
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be propagated when the ledger persistence fails")
  void shouldBePropagatedWhenLedgerPersistenceFails() {
    // (error: persistence failure)
    // Arrange
    Transaction ledgerEntry = Transaction.builder().operationNumber(OPERATION_NUMBER).build();
    when(yankiTransactionFactory.buildOperationNumber(TRANSACTION_ID))
            .thenReturn(OPERATION_NUMBER);
    when(transactionRepository.findByOperationNumber(OPERATION_NUMBER))
            .thenReturn(Maybe.empty());
    when(yankiTransactionFactory.buildLedgerEntry(command)).thenReturn(ledgerEntry);
    when(transactionRepository.save(ledgerEntry))
            .thenReturn(Single.error(new IllegalStateException("database unavailable")));

    // Act
    TestObserver<Void> observer = useCase.recordCompletedPayment(command).test();

    // Assert
    observer.assertError(IllegalStateException.class);
    observer.assertNotComplete();
  }
}

package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.in.FindTransactionByIdUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetrieveTransactionUseCaseImplTest {

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private FindTransactionByIdUseCase findTransactionByIdUseCase;

  private RetrieveTransactionUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new RetrieveTransactionUseCaseImpl(transactionRepository, findTransactionByIdUseCase);
  }

  @Test
  @DisplayName("Should return every transaction from the repository")
  void shouldFindAll() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction second = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    when(transactionRepository.findAll()).thenReturn(Flowable.just(first, second));

    // Act & Assert
    useCase.findAll()
            .test()
            .assertValues(first, second)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should delegate the lookup by ID to the find use case")
  void shouldDelegateFindById() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(findTransactionByIdUseCase.findById("tx-1")).thenReturn(Single.just(transaction));

    // Act & Assert
    useCase.findById("tx-1")
            .test()
            .assertValue(transaction)
            .assertComplete()
            .assertNoErrors();
    verify(findTransactionByIdUseCase).findById("tx-1");
  }

  @Test
  @DisplayName("Should return the transaction matching the operation number")
  void shouldFindByOperationNumberWhenPresent() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(transactionRepository.findByOperationNumber("OP-1"))
            .thenReturn(Maybe.just(transaction));

    // Act & Assert
    useCase.findByOperationNumber("OP-1")
            .test()
            .assertValue(transaction)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should emit a not found error when the operation number does not exist")
  void shouldEmitNotFoundWhenOperationNumberMissing() {
    // Arrange
    when(transactionRepository.findByOperationNumber("missing")).thenReturn(Maybe.empty());

    // Act & Assert
    useCase.findByOperationNumber("missing")
            .test()
            .assertError(TransactionNotFoundException.class)
            .assertError(hasErrorMessage("Transaction not found with operationNumber: missing"))
            .assertNotComplete();
  }

  @Test
  @DisplayName("Should return every transaction of a customer")
  void shouldFindByCustomerId() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(transactionRepository.findByCustomerId("cust-1"))
            .thenReturn(Flowable.just(transaction));

    // Act & Assert
    useCase.findByCustomerId("cust-1")
            .test()
            .assertValue(transaction)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should return every transaction of a target product")
  void shouldFindByTargetProductId() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(transactionRepository.findByTargetProductId("prod-1"))
            .thenReturn(Flowable.just(transaction));

    // Act & Assert
    useCase.findByTargetProductId("prod-1")
            .test()
            .assertValue(transaction)
            .assertComplete()
            .assertNoErrors();
    verifyNoInteractions(findTransactionByIdUseCase);
  }
}

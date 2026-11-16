package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindTransactionByIdUseCaseImplTest {

  @Mock
  private TransactionRepositoryPort transactionRepository;

  private FindTransactionByIdUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new FindTransactionByIdUseCaseImpl(transactionRepository);
  }

  @Test
  @DisplayName("Should return the transaction when it exists")
  void shouldReturnTransactionWhenFound() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(transactionRepository.findById("tx-1")).thenReturn(Maybe.just(transaction));

    // Act & Assert
    useCase.findById("tx-1")
            .test()
            .assertValue(transaction)
            .assertComplete()
            .assertNoErrors();
    verify(transactionRepository).findById("tx-1");
  }

  @Test
  @DisplayName("Should emit a not found error when the transaction does not exist")
  void shouldEmitNotFoundWhenMissing() {
    // Arrange
    when(transactionRepository.findById("missing")).thenReturn(Maybe.empty());

    // Act & Assert
    useCase.findById("missing")
            .test()
            .assertError(TransactionNotFoundException.class)
            .assertError(hasErrorMessage("Transaction not found with ID: missing"))
            .assertNotComplete();
  }
}

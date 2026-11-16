package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.in.FindTransactionByIdUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.TransactionExecutionStrategy;
import com.pe.pivot.transaction.domain.strategy.TransactionStrategyRegistry;
import com.pe.pivot.transaction.infrastructure.config.TransactionRulesProperties;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReverseTransactionUseCaseImplTest {

  @Mock
  private FindTransactionByIdUseCase findTransactionByIdUseCase;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private TransactionStrategyRegistry strategyRegistry;

  @Mock
  private TransactionRulesProperties rulesProperties;

  @Mock
  private TransactionExecutionStrategy strategy;

  private ReverseTransactionUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new ReverseTransactionUseCaseImpl(
            findTransactionByIdUseCase, transactionRepository, strategyRegistry, rulesProperties);
  }

  @Test
  @DisplayName("Should create a compensating transaction and mark the original as reversed")
  void shouldReverseCompletedTransaction() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction savedCompensating = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    stubSuccessfulReversal(original, savedCompensating);

    // Act & Assert
    useCase.reverse("tx-1", "Duplicate charge")
            .test()
            .assertValue(savedCompensating)
            .assertComplete()
            .assertNoErrors();

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(strategy).execute(captor.capture(), eq("OP-"), eq(BigDecimal.ZERO));
    Transaction compensating = captor.getValue();
    assertThat(compensating.getCustomerId()).isEqualTo("cust-1");
    assertThat(compensating.getTargetProductId()).isEqualTo("prod-1");
    assertThat(compensating.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    assertThat(compensating.getCurrency()).isEqualTo("PEN");
    assertThat(compensating.getAmount()).isEqualByComparingTo("100.00");
    assertThat(compensating.getFee()).isEqualByComparingTo("0");
    assertThat(compensating.getDescription()).isEqualTo("[REVERSAL] Duplicate charge");
    assertThat(compensating.getReferenceTransactionId()).isEqualTo("tx-1");
    assertThat(original.getStatus()).isEqualTo(TransactionStatus.REVERSED);
    assertThat(original.getUpdatedAt()).isNotNull();
    verify(transactionRepository).save(original);
  }

  @Test
  @DisplayName("Should use a default description when the reversal reason is null")
  void shouldUseDefaultDescriptionWhenReasonIsNull() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction savedCompensating = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    stubSuccessfulReversal(original, savedCompensating);

    // Act
    useCase.reverse("tx-1", null)
            .test()
            .assertValue(savedCompensating)
            .assertComplete()
            .assertNoErrors();

    // Assert
    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(strategy).execute(captor.capture(), eq("OP-"), eq(BigDecimal.ZERO));
    assertThat(captor.getValue().getDescription())
            .isEqualTo("[REVERSAL] Compensating reversal of operation "
                    + original.getOperationNumber());
  }

  @Test
  @DisplayName("Should use a default description when the reversal reason is blank")
  void shouldUseDefaultDescriptionWhenReasonIsBlank() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction savedCompensating = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    stubSuccessfulReversal(original, savedCompensating);

    // Act
    useCase.reverse("tx-1", "   ")
            .test()
            .assertValue(savedCompensating)
            .assertComplete()
            .assertNoErrors();

    // Assert
    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(strategy).execute(captor.capture(), eq("OP-"), eq(BigDecimal.ZERO));
    assertThat(captor.getValue().getDescription())
            .isEqualTo("[REVERSAL] Compensating reversal of operation "
                    + original.getOperationNumber());
  }

  @Test
  @DisplayName("Should reject a reversal when the transaction is already reversed")
  void shouldRejectWhenAlreadyReversed() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    original.setStatus(TransactionStatus.REVERSED);
    when(findTransactionByIdUseCase.findById("tx-1")).thenReturn(Single.just(original));

    // Act & Assert
    useCase.reverse("tx-1", "reason")
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Transaction cannot be reversed. It is already "
                    + "REVERSED or is itself a reversal entry."))
            .assertNotComplete();
    verifyNoInteractions(strategyRegistry);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should reject a reversal when the transaction is itself a compensating entry")
  void shouldRejectWhenItIsACompensatingEntry() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    original.setReferenceTransactionId("original-tx");
    when(findTransactionByIdUseCase.findById("tx-1")).thenReturn(Single.just(original));

    // Act & Assert
    useCase.reverse("tx-1", "reason")
            .test()
            .assertError(BusinessRuleException.class)
            .assertNotComplete();
    verifyNoInteractions(strategyRegistry);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate a not found error from the lookup")
  void shouldPropagateNotFound() {
    // Arrange
    when(findTransactionByIdUseCase.findById("missing"))
            .thenReturn(Single.error(new TransactionNotFoundException(
                    "Transaction not found with ID: missing")));

    // Act & Assert
    useCase.reverse("missing", "reason")
            .test()
            .assertError(TransactionNotFoundException.class)
            .assertError(hasErrorMessage("Transaction not found with ID: missing"));
    verifyNoInteractions(strategyRegistry);
  }

  @Test
  @DisplayName("Should propagate errors emitted by the compensating strategy")
  void shouldPropagateStrategyError() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(findTransactionByIdUseCase.findById("tx-1")).thenReturn(Single.just(original));
    when(rulesProperties.getOperationPrefix()).thenReturn("OP-");
    when(strategyRegistry.getStrategy(TransactionType.WITHDRAWAL)).thenReturn(strategy);
    when(strategy.execute(any(Transaction.class), eq("OP-"), eq(BigDecimal.ZERO)))
            .thenReturn(Single.error(new BusinessRuleException("Compensating failed")));

    // Act & Assert
    useCase.reverse("tx-1", "reason")
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Compensating failed"));
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should propagate errors when persisting the reversed original fails")
  void shouldPropagateSaveError() {
    // Arrange
    Transaction original = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction savedCompensating = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    when(findTransactionByIdUseCase.findById("tx-1")).thenReturn(Single.just(original));
    when(rulesProperties.getOperationPrefix()).thenReturn("OP-");
    when(strategyRegistry.getStrategy(TransactionType.WITHDRAWAL)).thenReturn(strategy);
    when(strategy.execute(any(Transaction.class), eq("OP-"), eq(BigDecimal.ZERO)))
            .thenReturn(Single.just(savedCompensating));
    when(transactionRepository.save(original))
            .thenReturn(Single.error(new IllegalStateException("Database unavailable")));

    // Act & Assert
    useCase.reverse("tx-1", "reason")
            .test()
            .assertError(IllegalStateException.class)
            .assertError(hasErrorMessage("Database unavailable"));
  }

  private void stubSuccessfulReversal(Transaction original, Transaction savedCompensating) {
    when(findTransactionByIdUseCase.findById(original.getId())).thenReturn(Single.just(original));
    when(rulesProperties.getOperationPrefix()).thenReturn("OP-");
    when(strategyRegistry.getStrategy(
            original.getTransactionType().getCompensatingTransactionType())).thenReturn(strategy);
    when(strategy.execute(any(Transaction.class), eq("OP-"), eq(BigDecimal.ZERO)))
            .thenReturn(Single.just(savedCompensating));
    when(transactionRepository.save(original)).thenReturn(Single.just(original));
  }
}

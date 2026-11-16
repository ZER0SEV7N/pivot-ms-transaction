package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterTransactionUseCaseImplTest {

  @Mock
  private TransactionStrategyRegistry strategyRegistry;

  @Mock
  private TransactionRulesProperties rulesProperties;

  @Mock
  private TransactionExecutionStrategy strategy;

  private RegisterTransactionUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new RegisterTransactionUseCaseImpl(strategyRegistry, rulesProperties);
  }

  @Test
  @DisplayName("Should delegate registration to the strategy that supports the transaction type")
  void shouldDelegateToStrategy() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction saved = TestData.transaction("tx-2", TransactionType.DEPOSIT);
    BigDecimal defaultFee = new BigDecimal("1.50");
    when(strategyRegistry.getStrategy(TransactionType.DEPOSIT)).thenReturn(strategy);
    when(rulesProperties.getOperationPrefix()).thenReturn("OP-");
    when(rulesProperties.getDefaultFee()).thenReturn(defaultFee);
    when(strategy.execute(transaction, "OP-", defaultFee)).thenReturn(Single.just(saved));

    // Act & Assert
    useCase.register(transaction)
            .test()
            .assertValue(saved)
            .assertComplete()
            .assertNoErrors();
    verify(strategy).execute(transaction, "OP-", defaultFee);
  }

  @Test
  @DisplayName("Should propagate errors emitted by the selected strategy")
  void shouldPropagateStrategyError() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    BigDecimal defaultFee = BigDecimal.ZERO;
    when(strategyRegistry.getStrategy(TransactionType.DEPOSIT)).thenReturn(strategy);
    when(rulesProperties.getOperationPrefix()).thenReturn("OP-");
    when(rulesProperties.getDefaultFee()).thenReturn(defaultFee);
    when(strategy.execute(transaction, "OP-", defaultFee))
            .thenReturn(Single.error(new BusinessRuleException("Transaction rejected")));

    // Act & Assert
    useCase.register(transaction)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Transaction rejected"))
            .assertNotComplete();
  }

  @Test
  @DisplayName("Should propagate the unsupported transaction type error from the registry")
  void shouldPropagateUnsupportedTypeError() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(strategyRegistry.getStrategy(TransactionType.DEPOSIT))
            .thenThrow(new BusinessRuleException("Unsupported transaction type: DEPOSIT"));

    // Act & Assert
    assertThatThrownBy(() -> useCase.register(transaction))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Unsupported transaction type: DEPOSIT");
    verifyNoInteractions(rulesProperties);
  }
}

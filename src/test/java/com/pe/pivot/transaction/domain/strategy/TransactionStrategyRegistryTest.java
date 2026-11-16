package com.pe.pivot.transaction.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.TransactionType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionStrategyRegistryTest {

  @Test
  @DisplayName("Should resolve every registered strategy by its supported type")
  void shouldResolveRegisteredStrategies() {
    // Arrange
    TransactionExecutionStrategy deposit = mock(TransactionExecutionStrategy.class);
    TransactionExecutionStrategy withdrawal = mock(TransactionExecutionStrategy.class);
    when(deposit.getSupportedType()).thenReturn(TransactionType.DEPOSIT);
    when(withdrawal.getSupportedType()).thenReturn(TransactionType.WITHDRAWAL);
    TransactionStrategyRegistry registry =
            new TransactionStrategyRegistry(List.of(deposit, withdrawal));

    // Act
    TransactionExecutionStrategy resolvedDeposit = registry.getStrategy(TransactionType.DEPOSIT);
    TransactionExecutionStrategy resolvedWithdrawal =
            registry.getStrategy(TransactionType.WITHDRAWAL);

    // Assert
    assertThat(resolvedDeposit).isSameAs(deposit);
    assertThat(resolvedWithdrawal).isSameAs(withdrawal);
  }

  @Test
  @DisplayName("Should throw a business rule exception for an unsupported transaction type")
  void shouldThrowForUnsupportedType() {
    // Arrange
    TransactionExecutionStrategy deposit = mock(TransactionExecutionStrategy.class);
    when(deposit.getSupportedType()).thenReturn(TransactionType.DEPOSIT);
    TransactionStrategyRegistry registry = new TransactionStrategyRegistry(List.of(deposit));

    // Act & Assert
    assertThatThrownBy(() -> registry.getStrategy(TransactionType.CREDIT_PAYMENT))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Unsupported transaction type: CREDIT_PAYMENT");
  }
}

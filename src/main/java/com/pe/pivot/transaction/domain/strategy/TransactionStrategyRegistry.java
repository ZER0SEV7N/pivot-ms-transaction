package com.pe.pivot.transaction.domain.strategy;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.TransactionType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry/Factory component providing O(1) lookup for TransactionExecutionStrategy implementations.
 */
public class TransactionStrategyRegistry {

  private final Map<TransactionType, TransactionExecutionStrategy> strategyMap;

  /**
   * Constructs a TransactionStrategyRegistry with a list of strategies, mapping each strategy to its supported TransactionType.
   *
   * @param strategies - List of TransactionExecutionStrategy implementations to register.
   */
  public TransactionStrategyRegistry(List<TransactionExecutionStrategy> strategies) {
    this.strategyMap = new EnumMap<>(TransactionType.class);
    strategies.forEach(strategy -> this.strategyMap.put(strategy.getSupportedType(), strategy));
  }

  /**
   * Retrieves the TransactionExecutionStrategy for the given TransactionType.
   *
   * @param type - The TransactionType for which to retrieve the strategy.
   * @return - The corresponding TransactionExecutionStrategy.
   */
  public TransactionExecutionStrategy getStrategy(TransactionType type) {
    return Optional.ofNullable(this.strategyMap.get(type))
            .orElseThrow(() -> new BusinessRuleException("Unsupported transaction type: " + type));
  }
}

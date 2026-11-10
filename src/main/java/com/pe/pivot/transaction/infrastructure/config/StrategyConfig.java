package com.pe.pivot.transaction.infrastructure.config;

import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.types.CreditConsumptionTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.CreditPaymentTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.DepositTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.TransactionExecutionStrategy;
import com.pe.pivot.transaction.domain.strategy.TransactionStrategyRegistry;
import com.pe.pivot.transaction.domain.strategy.types.WithdrawalTransactionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring Configuration wiring the 4 concrete Transaction Strategies and the Strategy Registry.
 */
@Configuration
public class StrategyConfig {

  /**
   * Creates a DepositTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   * - Validates that the customer is active.
   * - Updates the account balance by adding the deposit amount.
   * - Creates a new transaction record.
   *
   * @param customerClient - Client for customer-related operations.
   * @param transactionRepository - Repository for transaction persistence.
   * @param accountClient - Client for account-related operations.
   * @return - A configured instance of DepositTransactionStrategy.
   */
  @Bean
  public TransactionExecutionStrategy depositTransactionStrategy(CustomerClientPort customerClient, TransactionRepositoryPort transactionRepository, AccountClientPort accountClient) {
    return new DepositTransactionStrategy(customerClient, transactionRepository, accountClient);
  }

  /**
   * Creates a WithdrawalTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   * - Validates that the customer is active.
   * - Updates the account balance by subtracting the withdrawal amount.
   * - Creates a new transaction record.
   *
   * @param customerClient - Client for customer-related operations.
   * @param transactionRepository - Repository for transaction persistence.
   * @param accountClient - Client for account-related operations.
   * @return - A configured instance of WithdrawalTransactionStrategy.
   */
  @Bean
  public TransactionExecutionStrategy withdrawalTransactionStrategy( CustomerClientPort customerClient, TransactionRepositoryPort transactionRepository, AccountClientPort accountClient) {
    return new WithdrawalTransactionStrategy(customerClient, transactionRepository, accountClient);
  }

  /**
   * Creates a CreditPaymentTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   * - Validates that the customer is active.
   * - Updates the credit balance by subtracting the payment amount.
   * - Creates a new transaction record.
   *
   * @param customerClient - Client for customer-related operations.
   * @param transactionRepository - Repository for transaction persistence.
   * @param creditClient - Client for credit-related operations.
   * @return - A configured instance of CreditPaymentTransactionStrategy.
   */
  @Bean
  public TransactionExecutionStrategy creditPaymentTransactionStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          CreditClientPort creditClient) {
    return new CreditPaymentTransactionStrategy(customerClient, transactionRepository, creditClient);
  }

  /**
   * Creates a CreditConsumptionTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   * - Validates that the customer is active.
   * - Updates the credit balance by adding the consumption amount.
   * - Creates a new transaction record.
   *
   * @param customerClient - Client for customer-related operations.
   * @param transactionRepository - Repository for transaction persistence.
   * @param creditClient - Client for credit-related operations.
   * @return - A configured instance of CreditConsumptionTransactionStrategy.
   */
  @Bean
  public TransactionExecutionStrategy creditConsumptionTransactionStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          CreditClientPort creditClient) {
    return new CreditConsumptionTransactionStrategy(customerClient, transactionRepository, creditClient);
  }

  /**
   * Creates a TransactionStrategyRegistry bean that aggregates all available TransactionExecutionStrategy beans.
   *
   * @param strategies - List of all TransactionExecutionStrategy beans available in the Spring context.
   * @return - A configured instance of TransactionStrategyRegistry that can be used to retrieve the appropriate strategy based on transaction type.
   */
  @Bean
  public TransactionStrategyRegistry transactionStrategyRegistry(List<TransactionExecutionStrategy> strategies) {
    return new TransactionStrategyRegistry(strategies);
  }
}
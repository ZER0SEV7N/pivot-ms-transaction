package com.pe.pivot.transaction.infrastructure.config;

import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.DebitCardClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.TransactionExecutionStrategy;
import com.pe.pivot.transaction.domain.strategy.TransactionStrategyRegistry;
import com.pe.pivot.transaction.domain.strategy.types.credit.CreditConsumptionTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.credit.CreditPaymentTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.debitcard.DebitCardPaymentTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.debitcard.DebitCardWithdrawalTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.debitcard.ThirdPartyCreditPaymentStrategy;
import com.pe.pivot.transaction.domain.strategy.types.transaction.DepositTransactionStrategy;
import com.pe.pivot.transaction.domain.strategy.types.transaction.WithdrawalTransactionStrategy;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
  public TransactionExecutionStrategy depositTransactionStrategy(
      CustomerClientPort customerClient,
      TransactionRepositoryPort transactionRepository,
      AccountClientPort accountClient) {
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
  public TransactionExecutionStrategy withdrawalTransactionStrategy(
      CustomerClientPort customerClient,
      TransactionRepositoryPort transactionRepository,
      AccountClientPort accountClient) {
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
    return new CreditPaymentTransactionStrategy(
            customerClient, transactionRepository, creditClient);
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
    return new CreditConsumptionTransactionStrategy(
            customerClient, transactionRepository, creditClient);
  }

  /**
   * Creates a TransactionStrategyRegistry bean that aggregates all available
   * TransactionExecutionStrategy beans.
   *
   * @param strategies - List of all TransactionExecutionStrategy beans available in the context.
   * @return - A configured instance of TransactionStrategyRegistry that can be used to retrieve
   *     the appropriate strategy based on transaction type.
   */
  @Bean
  public TransactionStrategyRegistry transactionStrategyRegistry(
      List<TransactionExecutionStrategy> strategies) {
    return new TransactionStrategyRegistry(strategies);
  }

  /**
   * Creates a DebitCardPaymentTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   *  - Validates that the customer is active.
   *  - Updates the debit card balance by subtracting the payment amount.
   *  - Creates a new transaction record.
   *
   * @param customerClient - the port for interacting with customer data
   * @param transactionRepository - the port for interacting with transaction data
   * @param debitCardClient - the port for interacting with debit card data
   * @return - a configured instance of DebitCardPaymentTransactionStrategy
   */
  @Bean
  public TransactionExecutionStrategy debitCardPaymentTransactionStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          DebitCardClientPort debitCardClient) {
    return new DebitCardPaymentTransactionStrategy(
            customerClient, transactionRepository, debitCardClient);
  }

  /**
   * Creates a DebitCardWithdrawalTransactionStrategy bean with the required dependencies.
   * This Strategy rules:
   *  - Validates that the customer is active.
   *  - Updates the debit card balance by adding the withdrawal amount.
   *  - Creates a new transaction record.
   *
   * @param customerClient - the port for interacting with customer data
   * @param transactionRepository - the port for interacting with transaction data
   * @param debitCardClient - the port for interacting with debit card data
   * @return - a configured instance of DebitCardWithdrawalTransactionStrategy
   */
  @Bean
  public TransactionExecutionStrategy debitCardWithdrawalTransactionStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          DebitCardClientPort debitCardClient) {
    return new DebitCardWithdrawalTransactionStrategy(
            customerClient, transactionRepository, debitCardClient);
  }

  /**
   * Creates a ThirdPartyCreditPaymentStrategy bean with the required dependencies.
   * This Strategy rules:
   *  - Validates that the customer is active.
   *  - Updates the credit balance of a third-party product by subtracting the payment amount
   *  - Creates a new transaction record.
   *
   * @param customerClient - the port for interacting with customer data
   * @param transactionRepository - the port for interacting with transaction data
   * @param creditClient - the port for interacting with credit product data
   * @return - a configured instance of ThirdPartyCreditPaymentStrategy
   */
  @Bean
  public TransactionExecutionStrategy thirdPartyCreditPaymentStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          CreditClientPort creditClient) {
    return new ThirdPartyCreditPaymentStrategy(
            customerClient, transactionRepository, creditClient);
  }
}

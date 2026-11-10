package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.port.in.RegisterTransactionUseCase;
import com.pe.pivot.transaction.domain.strategy.TransactionStrategyRegistry;
import com.pe.pivot.transaction.infrastructure.config.TransactionRulesProperties;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case for orchestrating new financial transactions
 * via the TransactionStrategyRegistry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterTransactionUseCaseImpl implements RegisterTransactionUseCase {

  private final TransactionStrategyRegistry strategyRegistry;
  private final TransactionRulesProperties rulesProperties;

  /**
   * Registers a new transaction by delegating to the appropriate strategy based on the transaction type.
   *
   * @param transaction - The transaction object containing details such as type, customer ID, target product ID, and amount.
   * @return - A Single emitting the saved Transaction object upon successful completion.
   */
  @Override
  public Single<Transaction> register(Transaction transaction) {
    log.debug("Registering transaction of type {} for customerId: {} and targetProductId: {}",
            transaction.getTransactionType(), transaction.getCustomerId(), transaction.getTargetProductId());

    // Delegate the transaction execution to the appropriate strategy based on the transaction type
    return strategyRegistry.getStrategy(transaction.getTransactionType())
            .execute(transaction, rulesProperties.getOperationPrefix(), rulesProperties.getDefaultFee())
            .doOnSuccess(saved -> log.info("Transaction completed successfully. OperationNumber: {}, Type: {}, Amount: {}, NewBalance: {}",
                    saved.getOperationNumber(), saved.getTransactionType(), saved.getAmount(), saved.getNewBalance()));
  }
}
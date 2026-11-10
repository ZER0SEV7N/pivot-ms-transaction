package com.pe.pivot.transaction.domain.strategy.types;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.AbstractTransactionStrategy;
import io.reactivex.rxjava3.core.Single;

/**
 * Concrete Strategy for executing CREDIT_CONSUMPTION transactions against ms-credit (:8083).
 */
public class CreditConsumptionTransactionStrategy extends AbstractTransactionStrategy {

  private final CreditClientPort creditClient;

  public CreditConsumptionTransactionStrategy(CustomerClientPort customerClient, TransactionRepositoryPort transactionRepository, CreditClientPort creditClient) {
    super(customerClient, transactionRepository);
    this.creditClient = creditClient;
  }

  /**
   * Returns the TransactionType that this strategy supports, which is CREDIT_CONSUMPTION.
   *
   * @return - the supported TransactionType
   */
  @Override
  public TransactionType getSupportedType() {
    return TransactionType.CREDIT_CONSUMPTION;
  }

  /**
   * Executes the remote balance update for a CREDIT_CONSUMPTION transaction by calling the ms-credit service.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - a Single emitting the ProductBalanceSnapshot after the balance update
   */
  @Override
  protected Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction) {
    return creditClient.updateCreditBalance(
            transaction.getTargetProductId(),
            TransactionType.CREDIT_CONSUMPTION.getRemoteOperationName(),
            transaction.getAmount()
    );
  }
}
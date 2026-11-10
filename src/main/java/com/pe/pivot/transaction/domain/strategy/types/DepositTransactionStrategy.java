package com.pe.pivot.transaction.domain.strategy.types;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.AbstractTransactionStrategy;
import io.reactivex.rxjava3.core.Single;

/**
 * Concrete Strategy for executing bank account DEPOSIT transactions against ms-account (:8082)
 */
public class DepositTransactionStrategy extends AbstractTransactionStrategy {
  private final AccountClientPort accountClient;

  public DepositTransactionStrategy(CustomerClientPort customerClient, TransactionRepositoryPort transactionRepository, AccountClientPort accountClient) {
    super(customerClient, transactionRepository);
    this.accountClient = accountClient;
  }

  /**
   * Returns the TransactionType that this strategy supports (DEPOSIT).
   * @return TransactionType.DEPOSIT
   */
  @Override
  public TransactionType getSupportedType() {
    return TransactionType.DEPOSIT;
  }

  /**
   * Executes the remote balance update for a DEPOSIT transaction by calling the AccountClientPort to update the account balance.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - a Single emitting the ProductBalanceSnapshot after the balance update
   */
  @Override
  protected Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction) {
    return accountClient.updateAccountBalance(
            transaction.getTargetProductId(),
            TransactionType.DEPOSIT.getRemoteOperationName(),
            transaction.getAmount()
    );
  }
}

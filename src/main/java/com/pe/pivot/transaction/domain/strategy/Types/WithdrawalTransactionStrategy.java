package com.pe.pivot.transaction.domain.strategy.Types;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.AbstractTransactionStrategy;
import io.reactivex.rxjava3.core.Single;

/**
 * Concrete Strategy for executing bank account WITHDRAWAL transactions against ms-account (:8082).
 * This strategy handles the specific logic for processing withdrawals, including balance checks and ledger updates.
 */
public class WithdrawalTransactionStrategy extends AbstractTransactionStrategy {
  private final AccountClientPort accountClient;

  public WithdrawalTransactionStrategy(CustomerClientPort customerClient, TransactionRepositoryPort transactionRepository, AccountClientPort accountClient) {
    super(customerClient, transactionRepository);
    this.accountClient = accountClient;
  }

  /**
   * Returns the TransactionType that this strategy supports, which is WITHDRAWAL.
   *
   * @return - the TransactionType. WITHDRAWAL enum value
   */
  @Override
  public TransactionType getSupportedType() {
    return TransactionType.WITHDRAWAL;
  }

  /**
   * Executes the remote balance update for a withdrawal transaction by calling the ms-account service.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - a Single emitting the ProductBalanceSnapshot after the balance update
   */
  @Override
  protected Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction) {
    return accountClient.updateAccountBalance(
            transaction.getTargetProductId(),
            TransactionType.WITHDRAWAL.getRemoteOperationName(),
            transaction.getAmount()
    );
  }
}

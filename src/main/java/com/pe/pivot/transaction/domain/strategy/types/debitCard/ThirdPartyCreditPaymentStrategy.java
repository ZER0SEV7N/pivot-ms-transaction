package com.pe.pivot.transaction.domain.strategy.types.debitcard;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.AbstractTransactionStrategy;
import io.reactivex.rxjava3.core.Single;

/**
 * Strategy enabling any active customer to pay a third-party credit product in ms-credit (:8083).
 */
public class ThirdPartyCreditPaymentStrategy extends AbstractTransactionStrategy {

  private final CreditClientPort creditClient;

  /**
   * Constructor for ThirdPartyCreditPaymentStrategy, initializing the required ports for
   * customer, transaction repository, and credit client.
   *
   * @param customerClient the port for interacting with customer data
   * @param transactionRepository the port for interacting with transaction data
   * @param creditClient the port for interacting with credit product data
   */
  public ThirdPartyCreditPaymentStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          CreditClientPort creditClient) {
    super(customerClient, transactionRepository);
    this.creditClient = creditClient;
  }

  /**
   * Returns the supported transaction type for this strategy, which is THIRD_PARTY_CREDIT_PAYMENT.
   *
   * @return the supported TransactionType for this strategy
   */
  @Override
  public TransactionType getSupportedType() {
    return TransactionType.THIRD_PARTY_CREDIT_PAYMENT;
  }

  /**
   * Executes the remote balance update for a third-party credit payment transaction by
   * interacting with the credit client.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - Single emitting the ProductBalanceSnapshot after the balance update is processed
   */
  @Override
  protected Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction) {
    return creditClient.updateCreditBalance(
            transaction.getTargetProductId(),
            TransactionType.THIRD_PARTY_CREDIT_PAYMENT.getRemoteOperationName(),
            transaction.getAmount());
  }
}

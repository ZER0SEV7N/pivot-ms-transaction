package com.pe.pivot.transaction.domain.strategy.types.debitcard;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.DebitCardClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.AbstractTransactionStrategy;
import io.reactivex.rxjava3.core.Single;

/**
 * Strategy executing DEBIT_CARD_PAYMENT transactions via cascading debit in ms-account (:8082).
 */
public class DebitCardPaymentTransactionStrategy extends AbstractTransactionStrategy {

  //
  private final DebitCardClientPort debitCardClient;

  /**
   * Constructor for DebitCardPaymentTransactionStrategy, initializing the required ports for
   * customer, transaction repository, and debit card client.
   *
   * @param customerClient the port for interacting with customer data
   * @param transactionRepository the port for interacting with transaction data
   * @param debitCardClient the port for interacting with debit card payment operations
   */
  public DebitCardPaymentTransactionStrategy(
          CustomerClientPort customerClient,
          TransactionRepositoryPort transactionRepository,
          DebitCardClientPort debitCardClient) {
    super(customerClient, transactionRepository);
    this.debitCardClient = debitCardClient;
  }

  /**
   * Returns the supported transaction type for this strategy, which is DEBIT_CARD_PAYMENT.
   *
   * @return the supported TransactionType for this strategy
   */
  @Override
  public TransactionType getSupportedType() {
    return TransactionType.DEBIT_CARD_PAYMENT;
  }

  /**
   * Executes the remote balance update for a debit card payment transaction by interacting with
   * the debit card client.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - Single emitting the ProductBalanceSnapshot after the balance update is processed
   */
  @Override
  protected Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction) {
    return debitCardClient.payWithDebitCard(
            transaction.getTargetProductId(), transaction.getAmount());
  }
}
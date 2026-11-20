package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

/**
 * Output port for executing cascading debit card payments in ms-account (:8082).
 */
public interface DebitCardClientPort {

  /**
   * Executes a debit card payment by interacting with the ms-account service to update the
   * account balance.
   *
   * @param debitCardId the unique identifier of the debit card used for the payment
   * @param amount the amount to be paid using the debit card
   * @return a Single emitting the ProductBalanceSnapshot after the payment is processed, or an
   *     error if the operation fails
   */
  Single<ProductBalanceSnapshot> payWithDebitCard(String debitCardId, BigDecimal amount);
}
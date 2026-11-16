package com.pe.pivot.transaction.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Rich Enumeration of financial transaction types encapsulating product routing
 * and compensating reversal metadata (OCP).
 */
@Getter
@RequiredArgsConstructor
public enum TransactionType {
  DEPOSIT(ProductCategory.BANK_ACCOUNT, Op.DEPOSIT, Op.WITHDRAWAL),
  WITHDRAWAL(ProductCategory.BANK_ACCOUNT, Op.WITHDRAWAL, Op.DEPOSIT),
  CREDIT_PAYMENT(ProductCategory.CREDIT_PRODUCT, Op.PAYMENT, Op.CONSUMPTION),
  CREDIT_CONSUMPTION(ProductCategory.CREDIT_PRODUCT, Op.CONSUMPTION, Op.PAYMENT),
  TRANSFER_OUT(ProductCategory.BANK_ACCOUNT, Op.WITHDRAWAL, Op.DEPOSIT),
  TRANSFER_IN(ProductCategory.BANK_ACCOUNT, Op.DEPOSIT, Op.WITHDRAWAL);

  private final ProductCategory productCategory;
  private final String remoteOperationName;
  private final String inverseRemoteOperationName;

  /**
   * Returns the compensating transaction type used to reverse this transaction.
   *
   * @return the inverse transaction type
   */
  public TransactionType getCompensatingTransactionType() {
    return switch (this) {
      case DEPOSIT, TRANSFER_IN -> WITHDRAWAL;
      case WITHDRAWAL, TRANSFER_OUT -> DEPOSIT;
      case CREDIT_PAYMENT -> CREDIT_CONSUMPTION;
      case CREDIT_CONSUMPTION -> CREDIT_PAYMENT;
    };
  }

  private static final class Op {
    private static final String DEPOSIT = "DEPOSIT";
    private static final String WITHDRAWAL = "WITHDRAWAL";
    private static final String PAYMENT = "PAYMENT";
    private static final String CONSUMPTION = "CONSUMPTION";
  }
}

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
  DEPOSIT(ProductCategory.BANK_ACCOUNT, "DEPOSIT", "WITHDRAWAL"),
  WITHDRAWAL(ProductCategory.BANK_ACCOUNT, "WITHDRAWAL", "DEPOSIT"),
  CREDIT_PAYMENT(ProductCategory.CREDIT_PRODUCT, "PAYMENT", "CONSUMPTION"),
  CREDIT_CONSUMPTION(ProductCategory.CREDIT_PRODUCT, "CONSUMPTION", "PAYMENT"),
  TRANSFER_OUT(ProductCategory.BANK_ACCOUNT, "WITHDRAWAL", "DEPOSIT"),
  TRANSFER_IN(ProductCategory.BANK_ACCOUNT, "DEPOSIT", "WITHDRAWAL");

  private final ProductCategory productCategory;
  private final String remoteOperationName;
  private final String inverseRemoteOperationName;

  public TransactionType getCompensatingTransactionType() {
    return switch (this) {
      case DEPOSIT, TRANSFER_IN -> WITHDRAWAL;
      case WITHDRAWAL, TRANSFER_OUT -> DEPOSIT;
      case CREDIT_PAYMENT -> CREDIT_CONSUMPTION;
      case CREDIT_CONSUMPTION -> CREDIT_PAYMENT;
    };
  }
}
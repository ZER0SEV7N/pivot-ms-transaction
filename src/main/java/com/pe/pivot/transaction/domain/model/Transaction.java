package com.pe.pivot.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Core Domain Entity representing an immutable financial ledger entry.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

  private String id;
  private String operationNumber;
  private String customerId;
  private String targetProductId;
  private String maskedProductNumber;
  private ProductCategory productCategory;
  private TransactionType transactionType;
  private String currency;
  private BigDecimal amount;
  private BigDecimal fee;
  private BigDecimal previousBalance;
  private BigDecimal newBalance;
  private String description;
  private String referenceTransactionId;
  private TransactionStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  public boolean canBeReversed() {
    return this.status == TransactionStatus.COMPLETED && this.referenceTransactionId == null;
  }
}
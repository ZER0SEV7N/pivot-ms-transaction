package com.pe.pivot.transaction.infrastructure.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MongoDB Document representing an immutable financial ledger record in 'db_pivot_transaction'.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class TransactionDocument {

  @Id
  private String id;

  @Indexed(unique = true)
  private String operationNumber;

  @Indexed
  private String customerId;

  @Indexed
  private String targetProductId;

  private String maskedProductNumber;
  private String productCategory;
  private String transactionType;
  private String currency;
  private BigDecimal amount;
  private BigDecimal fee;
  private BigDecimal previousBalance;
  private BigDecimal newBalance;
  private String description;
  private String referenceTransactionId;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;
}

package com.pe.pivot.transaction.support;

import com.pe.pivot.transaction.domain.model.CustomerSummary;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shared test data builders that keep each test focused on its own behavior.
 */
public final class TestData {

  public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

  private TestData() {
  }

  public static Transaction transaction(String id, TransactionType type) {
    return Transaction.builder()
            .id(id)
            .operationNumber("OP-20260115-" + id)
            .customerId("cust-1")
            .targetProductId("prod-1")
            .maskedProductNumber("****1234")
            .productCategory(type.getProductCategory())
            .transactionType(type)
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .fee(new BigDecimal("1.00"))
            .previousBalance(new BigDecimal("500.00"))
            .newBalance(new BigDecimal("600.00"))
            .description("Sample transaction")
            .status(TransactionStatus.COMPLETED)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build();
  }

  public static Transaction transactionAt(
          String id, Instant createdAt, BigDecimal amount, BigDecimal fee) {
    return Transaction.builder()
            .id(id)
            .maskedProductNumber("mask-" + id)
            .productCategory(ProductCategory.BANK_ACCOUNT)
            .currency("PEN")
            .amount(amount)
            .fee(fee)
            .createdAt(createdAt)
            .build();
  }

  public static CustomerSummary customer(String status) {
    return CustomerSummary.builder()
            .id("cust-1")
            .customerType("NATURAL")
            .documentNumber("12345678")
            .status(status)
            .build();
  }

  public static CustomerSummary activeCustomer() {
    return customer("ACTIVE");
  }

  public static ProductBalanceSnapshot snapshot() {
    return ProductBalanceSnapshot.builder()
            .productId("prod-1")
            .maskedProductNumber("****1234")
            .currency("PEN")
            .previousBalance(new BigDecimal("500.00"))
            .newBalance(new BigDecimal("600.00"))
            .appliedCommission(new BigDecimal("1.00"))
            .build();
  }

  public static TransferCommand transferCommand() {
    return TransferCommand.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .description("Transfer description")
            .build();
  }
}

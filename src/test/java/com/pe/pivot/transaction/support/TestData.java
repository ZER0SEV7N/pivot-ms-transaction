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

  /**
   * Builds a completed transaction with every field populated.
   *
   * @param id transaction identifier
   * @param type transaction type that determines the product category
   * @return a fully populated transaction
   */
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

  /**
   * Builds a bank-account transaction with the supplied timestamps and amounts.
   *
   * @param id transaction identifier
   * @param createdAt creation timestamp used for ordering and report calculations
   * @param amount transaction amount
   * @param fee commission applied to the transaction
   * @return a transaction ready for report and ordering assertions
   */
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

  /**
   * Builds a customer summary with the supplied status.
   *
   * @param status customer status such as ACTIVE or INACTIVE
   * @return a customer summary projection
   */
  public static CustomerSummary customer(String status) {
    return CustomerSummary.builder()
            .id("cust-1")
            .customerType("NATURAL")
            .documentNumber("12345678")
            .status(status)
            .build();
  }

  /**
   * Builds an active customer summary.
   *
   * @return a customer summary whose status is ACTIVE
   */
  public static CustomerSummary activeCustomer() {
    return customer("ACTIVE");
  }

  /**
   * Builds a product balance snapshot used by the remote client adapters.
   *
   * @return a bank-account balance snapshot
   */
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

  /**
   * Builds a transfer command between two bank accounts.
   *
   * @return a transfer command ready to be executed
   */
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

package com.pe.pivot.transaction.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.support.TestData;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionReportBuilderTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant END = Instant.parse("2026-01-31T23:59:59Z");

  private TransactionReportBuilder reportBuilder;

  @BeforeEach
  void setUp() {
    reportBuilder = new TransactionReportBuilder();
  }

  @Test
  @DisplayName("Should aggregate totals, filter by date and sort movements descending")
  void shouldBuildProductReportWithFilteredAndAggregatedData() {
    // Arrange
    Transaction insideLate = TestData.transactionAt(
            "late", END.minus(1, ChronoUnit.HOURS), new BigDecimal("200.00"), null);
    Transaction insideNullValues = TestData.transactionAt(
            "nulls", START.plus(2, ChronoUnit.HOURS), null, null);
    Transaction insideEarly = TestData.transactionAt(
            "early", START.plus(1, ChronoUnit.HOURS), new BigDecimal("100.00"),
            new BigDecimal("1.00"));
    Transaction before = TestData.transactionAt(
            "before", START.minusSeconds(1), new BigDecimal("50.00"), new BigDecimal("0.50"));
    Transaction after = TestData.transactionAt(
            "after", END.plusSeconds(1), new BigDecimal("70.00"), new BigDecimal("0.70"));
    Transaction withoutDate = TestData.transactionAt(
            "no-date", null, new BigDecimal("80.00"), new BigDecimal("0.80"));
    List<Transaction> allTransactions =
            List.of(insideEarly, insideLate, insideNullValues, before, after, withoutDate);

    // Act
    ProductReportSummary result =
            reportBuilder.buildProductReport("prod-1", START, END, allTransactions);

    // Assert
    assertThat(result.getProductId()).isEqualTo("prod-1");
    assertThat(result.getStartDate()).isEqualTo(START);
    assertThat(result.getEndDate()).isEqualTo(END);
    assertThat(result.getTotalTransactions()).isEqualTo(3);
    assertThat(result.getTotalTransactedAmount()).isEqualByComparingTo("300.00");
    assertThat(result.getTotalFeesCollected()).isEqualByComparingTo("1.00");
    assertThat(result.getMovements()).containsExactly(insideLate, insideNullValues, insideEarly);
    assertThat(result.getMaskedProductNumber()).isEqualTo("mask-late");
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(result.getCurrency()).isEqualTo("PEN");
  }

  @Test
  @DisplayName("Should fall back to the first available transaction when no movement matches")
  void shouldFallBackToFirstAvailableTransactionWhenNoMovementMatches() {
    // Arrange
    Transaction outside = TestData.transactionAt(
            "outside", START.minus(1, ChronoUnit.DAYS), new BigDecimal("50.00"),
            new BigDecimal("0.50"));
    List<Transaction> allTransactions = List.of(outside);

    // Act
    ProductReportSummary result =
            reportBuilder.buildProductReport("prod-1", START, END, allTransactions);

    // Assert
    assertThat(result.getTotalTransactions()).isZero();
    assertThat(result.getTotalTransactedAmount()).isEqualByComparingTo("0");
    assertThat(result.getTotalFeesCollected()).isEqualByComparingTo("0");
    assertThat(result.getMovements()).isEmpty();
    assertThat(result.getMaskedProductNumber()).isEqualTo("mask-outside");
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(result.getCurrency()).isEqualTo("PEN");
  }

  @Test
  @DisplayName("Should use default values when there are no transactions at all")
  void shouldUseDefaultsWhenThereAreNoTransactions() {
    // Arrange
    List<Transaction> allTransactions = List.of();

    // Act
    ProductReportSummary result =
            reportBuilder.buildProductReport("prod-1", START, END, allTransactions);

    // Assert
    assertThat(result.getTotalTransactions()).isZero();
    assertThat(result.getTotalTransactedAmount()).isEqualByComparingTo("0");
    assertThat(result.getTotalFeesCollected()).isEqualByComparingTo("0");
    assertThat(result.getMovements()).isEmpty();
    assertThat(result.getMaskedProductNumber()).isNull();
    assertThat(result.getProductCategory()).isNull();
    assertThat(result.getCurrency()).isEqualTo("PEN");
  }

  @Test
  @DisplayName("Should return only the ten most recent dated movements in descending order")
  void shouldExtractLastTenMovementsInDescendingOrder() {
    // Arrange
    List<Transaction> transactions = List.of(
            TestData.transactionAt("tx-0", START.plus(0, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-1", START.plus(1, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-2", START.plus(2, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-3", START.plus(3, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-4", START.plus(4, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-5", START.plus(5, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-6", START.plus(6, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-7", START.plus(7, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-8", START.plus(8, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-9", START.plus(9, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-10", START.plus(10, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("tx-11", START.plus(11, ChronoUnit.HOURS), BigDecimal.ONE,
                    BigDecimal.ZERO),
            TestData.transactionAt("no-date", null, BigDecimal.ONE, BigDecimal.ZERO));

    // Act
    List<Transaction> result = reportBuilder.extractLastTenMovements(transactions);

    // Assert
    assertThat(result).hasSize(10);
    assertThat(result).extracting(Transaction::getId)
            .containsExactly("tx-11", "tx-10", "tx-9", "tx-8", "tx-7",
                    "tx-6", "tx-5", "tx-4", "tx-3", "tx-2");
  }
}

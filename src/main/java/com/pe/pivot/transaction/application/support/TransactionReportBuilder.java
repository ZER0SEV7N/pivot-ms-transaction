package com.pe.pivot.transaction.application.support;

import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Support Builder leveraging Java Streams to aggregate product reports and card movement histories.
 */
@Component
public class TransactionReportBuilder {

  private static final int MAX_RECENT_MOVEMENTS = 10; // Limit for recent card movements

  /**
   * Builds a ProductReportSummary for a given product over a specified date range, aggregating transaction data.
   *
   * @param productId - the unique identifier of the product for which the report is generated
   * @param startDate - the start of the reporting period (inclusive)
   * @param endDate - the end of the reporting period (inclusive)
   * @param allTransactions - the complete list of transactions to filter and aggregate for the report
   * @return - a ProductReportSummary containing aggregated data and filtered transactions
   */
  public ProductReportSummary buildProductReport(String productId, Instant startDate, Instant endDate, List<Transaction> allTransactions) {
    // Filter transactions for the specified product and date range
    List<Transaction> filteredMovements = allTransactions.stream()
            .filter(tx -> tx.getCreatedAt() != null && !tx.getCreatedAt().isBefore(startDate) && !tx.getCreatedAt().isAfter(endDate))
            .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
            .toList();

    BigDecimal totalAmount = filteredMovements.stream()
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalFees = filteredMovements.stream()
            .map(Transaction::getFee)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Transaction sample = null;
    if (!filteredMovements.isEmpty()) {
      sample = filteredMovements.get(0);
    } else if (!allTransactions.isEmpty()) {
      sample = allTransactions.get(0);
    }

    return ProductReportSummary.builder()
            .productId(productId)
            .maskedProductNumber(sample != null ? sample.getMaskedProductNumber() : null)
            .productCategory(sample != null ? sample.getProductCategory() : null)
            .currency(sample != null ? sample.getCurrency() : "PEN")
            .startDate(startDate)
            .endDate(endDate)
            .totalTransactions(filteredMovements.size())
            .totalTransactedAmount(totalAmount)
            .totalFeesCollected(totalFees)
            .movements(filteredMovements)
            .build();
  }

  /**
   * Extracts the last ten movements from a list of transactions, sorted by creation date in descending order.
   *
   * @param transactions - the complete list of transactions to filter and sort
   * @return - a list containing the last ten transactions based on creation date
   */
  public List<Transaction> extractLastTenMovements(List<Transaction> transactions) {
    return transactions.stream()
            .filter(tx -> tx.getCreatedAt() != null)
            .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
            .limit(MAX_RECENT_MOVEMENTS)
            .toList();
  }
}

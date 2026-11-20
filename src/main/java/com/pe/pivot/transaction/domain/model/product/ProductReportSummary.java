package com.pe.pivot.transaction.domain.model.product;

import com.pe.pivot.transaction.domain.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain model representing a consolidated financial product report over a date range.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReportSummary {

  private String productId;
  private String maskedProductNumber;
  private ProductCategory productCategory;
  private String currency;
  private Instant startDate;
  private Instant endDate;
  private Integer totalTransactions;
  private BigDecimal totalTransactedAmount;
  private BigDecimal totalFeesCollected;
  private List<Transaction> movements;
}

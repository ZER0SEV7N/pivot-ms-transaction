package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Immutable Response DTO representing a consolidated product report over a date range.
 */
@Builder
public record ProductReportResponse(
        String productId,
        String maskedProductNumber,
        ProductCategory productCategory,
        String currency,
        Instant startDate,
        Instant endDate,
        Integer totalTransactions,
        BigDecimal totalTransactedAmount,
        BigDecimal totalFeesCollected,
        List<TransactionResponse> movements
) {
}

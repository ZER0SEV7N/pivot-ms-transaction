package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable Response DTO representing a financial transaction ledger entry.
 */
@Builder
public record TransactionResponse(
        String id,
        String operationNumber,
        String customerId,
        String targetProductId,
        String maskedProductNumber,
        ProductCategory productCategory,
        TransactionType transactionType,
        String currency,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String description,
        String referenceTransactionId,
        TransactionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
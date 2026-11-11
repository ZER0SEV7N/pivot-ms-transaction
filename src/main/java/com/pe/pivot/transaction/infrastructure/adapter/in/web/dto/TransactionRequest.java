package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import com.pe.pivot.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable Request DTO using Java 17 Record for registering a financial transaction.
 */
@Builder
public record TransactionRequest(
        @NotBlank(message = "customerId is required")
        String customerId,

        @NotBlank(message = "targetProductId is required (Account ID or Credit ID)")
        String targetProductId,

        @NotNull(message = "transactionType is required (DEPOSIT, WITHDRAWAL, CREDIT_PAYMENT, CREDIT_CONSUMPTION)")
        TransactionType transactionType,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount,

        String description
) {
}
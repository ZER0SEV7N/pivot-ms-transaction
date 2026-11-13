package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable Request DTO for executing a bank transfer between two accounts.
 */
@Builder
public record TransferRequest(
        @NotBlank(message = "customerId is required")
        String customerId,

        @NotBlank(message = "sourceAccountId is required")
        String sourceAccountId,

        @NotBlank(message = "destinationAccountId is required")
        String destinationAccountId,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount,

        String description
) {
}
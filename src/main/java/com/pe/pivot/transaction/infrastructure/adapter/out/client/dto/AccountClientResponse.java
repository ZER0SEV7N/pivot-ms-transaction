package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable DTO representing the JSON response from ms-account (:8082) after a PATCH /balance.
 */
@Builder
public record AccountClientResponse(
        String id,
        String accountNumber,
        String customerId,
        String accountType,
        String currency,
        BigDecimal balance,
        String status
) {
}
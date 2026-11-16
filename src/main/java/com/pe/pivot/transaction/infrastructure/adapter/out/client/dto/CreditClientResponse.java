package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Immutable DTO representing the JSON response from ms-credit (:8083) after a PATCH /balance.
 */
@Builder
public record CreditClientResponse(
        String id,
        String creditNumber,
        String customerId,
        String creditType,
        String currency,
        BigDecimal creditLimit,
        BigDecimal usedAmount,
        BigDecimal availableAmount,
        String status
) {
}

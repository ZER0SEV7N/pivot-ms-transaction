package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable payload sent to PATCH /{id}/balance on ms-account or ms-credit.
 */
@Builder
public record RemoteBalanceUpdateRequest(
        String operationType,
        BigDecimal amount
) {
}
package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Immutable payload sent to PATCH /{id}/balance on ms-account or ms-credit.
 */
@Builder
public record RemoteBalanceUpdateRequest(
        String operationType,
        BigDecimal amount
) {
}

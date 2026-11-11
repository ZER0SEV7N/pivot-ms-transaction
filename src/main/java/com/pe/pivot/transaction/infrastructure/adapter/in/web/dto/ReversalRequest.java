package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import lombok.Builder;

/**
 * Optional Request DTO providing a reason for a compensating transaction reversal.
 */
@Builder
public record ReversalRequest(
        String reason
) {
}
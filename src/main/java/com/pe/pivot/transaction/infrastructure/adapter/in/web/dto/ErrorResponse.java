package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import lombok.Builder;

/**
 * Standardized API error response structure.
 */
@Builder
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}

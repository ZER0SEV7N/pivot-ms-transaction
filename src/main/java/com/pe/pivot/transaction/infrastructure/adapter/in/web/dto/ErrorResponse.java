package com.pe.pivot.transaction.infrastructure.adapter.in.web.dto;

import lombok.Builder;

import java.time.Instant;

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
package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import lombok.Builder;

/**
 * Immutable DTO representing the JSON response from ms-customer (:8081).
 */
@Builder
public record CustomerClientResponse(
        String id,
        String customerType,
        String profile,
        String documentType,
        String documentNumber,
        String status
) {
}
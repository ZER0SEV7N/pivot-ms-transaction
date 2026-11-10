package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

/**
 * Captures the error message returned by downstream microservices (ms-account / ms-credit).
 */
public record RemoteErrorResponse(
        String message
) {
}
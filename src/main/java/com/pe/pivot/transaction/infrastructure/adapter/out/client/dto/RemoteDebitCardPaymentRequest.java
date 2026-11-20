package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import java.math.BigDecimal;
import lombok.Builder;

/**
 * Request payload sent to ms-account PATCH /api/v1/debit-cards/{id}/pay.
 */
@Builder
public record RemoteDebitCardPaymentRequest(
        BigDecimal amount
) { }
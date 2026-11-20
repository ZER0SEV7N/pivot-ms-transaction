package com.pe.pivot.transaction.infrastructure.adapter.in.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Kafka event consumed from yanki.payment.result to record settled Yanki payments in the ledger.
 */
@Builder
public record YankiPaymentResultEvent(
        String transactionId,
        String status,
        String senderPhoneNumber,
        String receiverPhoneNumber,
        String senderDebitCardId,
        String receiverDebitCardId,
        String debitedAccountId,
        String creditedAccountId,
        String currency,
        BigDecimal amount,
        String failureReason,
        Instant processedAt
) {
}
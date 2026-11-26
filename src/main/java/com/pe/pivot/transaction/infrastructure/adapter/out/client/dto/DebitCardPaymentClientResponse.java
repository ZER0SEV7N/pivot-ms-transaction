package com.pe.pivot.transaction.infrastructure.adapter.out.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Response payload returned by ms-account PATCH /api/v1/debit-cards/{id}/pay.
 */
@Builder
public record DebitCardPaymentClientResponse(
        String debitCardId,
        String maskedCardNumber,
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        List<DebitedAccountItem> debitedAccounts,
        Instant processedAt
) {

  /**
   * Breakdown of a single account debited during a cascading debit card payment.
   */
  public record DebitedAccountItem(
          String accountId,
          String maskedAccountNumber,
          BigDecimal debitedAmount,
          BigDecimal appliedCommission,
          BigDecimal previousBalance,
          BigDecimal newBalance
  ) {
  }
}
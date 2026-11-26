package com.pe.pivot.transaction.domain.model.yanki;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * Domain command carrying a settled Yanki payment event to be recorded in the ledger.
 */
@Builder
public record YankiLedgerCommand(
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
        Instant processedAt
) {

  // Constants
  private static final String STATUS_COMPLETED = "COMPLETED";

  /**
   * Checks whether the Yanki payment settled successfully and should be recorded in the ledger.
   *
   * @return true if status is COMPLETED
   */
  public boolean isCompleted() {
    return STATUS_COMPLETED.equalsIgnoreCase(this.status);
  }
}
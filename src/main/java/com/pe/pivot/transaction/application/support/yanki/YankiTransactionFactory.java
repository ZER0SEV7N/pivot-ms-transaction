package com.pe.pivot.transaction.application.support.yanki;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Factory encapsulating the construction of immutable Yanki ledger entries (SRP).
 */
@Component
public class YankiTransactionFactory {

  private static final String YANKI_OP_PREFIX = "YK-";
  private static final String DEFAULT_CURRENCY = "PEN";

  /**
   * Builds a deterministic operation number to guarantee Kafka consumer idempotency.
   *
   * @param transactionId the Yanki transaction identifier
   * @return deterministic operation number
   */
  public String buildOperationNumber(String transactionId) {
    return YANKI_OP_PREFIX + transactionId;
  }

  /**
   * Creates an immutable COMPLETED Transaction entity from a settled YankiLedgerCommand.
   *
   * @param command the completed Yanki payment command
   * @return fully populated Transaction ready for ledger persistence
   */
  public Transaction buildLedgerEntry(YankiLedgerCommand command) {
    Instant timestamp = Optional.ofNullable(command.processedAt()).orElseGet(Instant::now);
    String targetProduct = resolveTargetProductId(command);
    String description = String.format("Yanki payment from %s to %s",
            maskPhone(command.senderPhoneNumber()), maskPhone(command.receiverPhoneNumber()));

    // Build the Transaction entity with all required fields populated
    return Transaction.builder()
            .operationNumber(buildOperationNumber(command.transactionId()))
            .customerId(command.senderPhoneNumber())
            .targetProductId(targetProduct)
            .maskedProductNumber(maskPhone(command.receiverPhoneNumber()))
            .productCategory(ProductCategory.YANKI_WALLET)
            .transactionType(TransactionType.YANKI_PAYMENT)
            .currency(Optional.ofNullable(command.currency()).orElse(DEFAULT_CURRENCY))
            .amount(command.amount())
            .fee(BigDecimal.ZERO)
            .previousBalance(BigDecimal.ZERO)
            .newBalance(BigDecimal.ZERO)
            .description(description)
            .status(TransactionStatus.COMPLETED)
            .createdAt(timestamp)
            .updatedAt(timestamp)
            .build();
  }

  /**
   * Resolves the target product ID for the transaction based on available command fields.
   *
   * @param command the YankiLedgerCommand containing transaction details
   * @return the resolved target product ID, which could be a debit card ID or a phone number
   */
  private String resolveTargetProductId(YankiLedgerCommand command) {
    // Check for sender debit card ID first
    if (command.senderDebitCardId() != null && !command.senderDebitCardId().isBlank()) {
      return command.senderDebitCardId();
    }
    // If sender debit card ID is not available, check for sender phone number
    if (command.receiverDebitCardId() != null && !command.receiverDebitCardId().isBlank()) {
      return command.receiverDebitCardId();
    }
    return command.receiverPhoneNumber();
  }

  /**
   * Masks a phone number for privacy, showing only the last 4 digits.
   *
   * @param phone the phone number to mask
   * @return the masked phone number
   */
  private String maskPhone(String phone) {
    // If the phone number is null or too short, return a default masked value
    if (phone == null || phone.length() < 4) {
      return "****";
    }

    // Mask all but the last 4 digits of the phone number
    int visible = 4;
    return "*".repeat(phone.length() - visible) + phone.substring(phone.length() - visible);
  }
}
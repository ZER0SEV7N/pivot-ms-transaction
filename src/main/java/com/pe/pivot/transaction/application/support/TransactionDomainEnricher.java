package com.pe.pivot.transaction.application.support;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.infrastructure.config.TransactionRulesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain Enricher / Factory responsible for constructing immutable ledger entries for bank transfers.
 *
 */
@Component
@RequiredArgsConstructor
public class TransactionDomainEnricher {

  // Creates a new Transaction domain object based on the provided TransferCommand and ProductBalanceSnapshot.
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final TransactionRulesProperties rules;

  /**
   * Builds a Transaction entry for a transfer operation, enriching it with details from the TransferCommand,
   * the target account ID, the transaction type, the product balance snapshot, and a reference ID.
   * The resulting Transaction object captures the state of the transfer for ledger purposes.
   *
   * @param cmd - the TransferCommand containing source and destination account details, amount, and description
   * @param targetAccountId - the ID of the account receiving the transfer
   * @param type - the type of transaction (e.g., DEBIT or CREDIT)
   * @param snapshot - the ProductBalanceSnapshot capturing the state of the product at the time of the transfer
   * @param referenceId - a unique reference ID for the transaction, used for reconciliation and tracking
   * @return - a fully constructed Transaction object representing the transfer entry
   */
  public Transaction buildTransferEntry(TransferCommand cmd, String targetAccountId, TransactionType type, ProductBalanceSnapshot snapshot, String referenceId) {
    Instant now = Instant.now();
    BigDecimal fee = Optional.ofNullable(snapshot.getAppliedCommission()).orElse(BigDecimal.ZERO);
    String defaultDesc = type == TransactionType.TRANSFER_OUT
            ? "Bank transfer sent to account " + cmd.getDestinationAccountId()
            : "Bank transfer received from account " + cmd.getSourceAccountId();

    // Construct and return a new Transaction object with all relevant details populated.
    return Transaction.builder()
            .operationNumber(generateOperationNumber())
            .customerId(cmd.getCustomerId())
            .targetProductId(targetAccountId)
            .maskedProductNumber(snapshot.getMaskedProductNumber())
            .productCategory(ProductCategory.BANK_ACCOUNT)
            .transactionType(type)
            .currency(Optional.ofNullable(cmd.getCurrency()).orElse(snapshot.getCurrency()))
            .amount(cmd.getAmount())
            .fee(fee)
            .previousBalance(snapshot.getPreviousBalance())
            .newBalance(snapshot.getNewBalance())
            .description(Optional.ofNullable(cmd.getDescription()).filter(d -> !d.isBlank()).orElse(defaultDesc))
            .referenceTransactionId(referenceId)
            .status(TransactionStatus.COMPLETED)
            .createdAt(now)
            .updatedAt(now)
            .build();
  }

  /**
   * Generates a unique operation number for the transaction, combining a prefix from configuration,
   *
   * @return - a string representing the unique operation number in the format: PREFIXYYYYMMDD-RANDOM
   */
  private String generateOperationNumber() {
    String datePart = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
    String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return rules.getOperationPrefix() + datePart + "-" + randomPart;
  }
}

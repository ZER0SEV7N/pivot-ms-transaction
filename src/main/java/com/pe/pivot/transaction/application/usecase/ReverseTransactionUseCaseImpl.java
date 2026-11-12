package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.in.FindTransactionByIdUseCase;
import com.pe.pivot.transaction.domain.port.in.ReverseTransactionUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.TransactionStrategyRegistry;
import com.pe.pivot.transaction.infrastructure.config.TransactionRulesProperties;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Single Responsibility Use Case for executing compensating transaction reversals
 * in compliance with Immutable Financial Ledger principles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReverseTransactionUseCaseImpl implements ReverseTransactionUseCase {

  private final FindTransactionByIdUseCase findTransactionByIdUseCase;
  private final TransactionRepositoryPort transactionRepository;
  private final TransactionStrategyRegistry strategyRegistry;
  private final TransactionRulesProperties rulesProperties;

  /**
   * Executes a compensating reversal for a given transaction ID, creating a new transaction that negates the effects of the original.
   *
   * @param transactionId - the ID of the transaction to be reversed
   * @param reason - the reason for the reversal, which will be included in the description of the compensating transaction
   * @return - a Single emitting the newly created compensating transaction after successful reversal
   */
  @Override
  public Single<Transaction> reverse(String transactionId, String reason) {
    log.debug("Initiating compensating reversal for transactionId: {}", transactionId);

    // Validate that the original transaction exists and can be reversed
    return findTransactionByIdUseCase.findById(transactionId)
            .flatMap(original -> {
              if (!original.canBeReversed()) {
                return Single.error(new BusinessRuleException("Transaction cannot be reversed. It is already REVERSED or is itself a reversal entry."));
              }

              // Build the compensating transaction based on the original transaction and the provided reason
              Transaction compensatingTx = buildCompensatingTransaction(original, reason);
              TransactionType inverseType = original.getTransactionType().getCompensatingTransactionType();

              // Execute the compensating transaction using the appropriate strategy based on the inverse transaction type
              return strategyRegistry.getStrategy(inverseType)
                      .execute(compensatingTx, rulesProperties.getOperationPrefix(), BigDecimal.ZERO)
                      .flatMap(savedCompensatingTx -> markOriginalAsReversed(original)
                              .map(ignored -> savedCompensatingTx));

            })
            .doOnSuccess(reversed -> log.info(
                    "Transaction reversed successfully. Compensating OperationNumber: {}, ReferenceTxId: {}",
                    reversed.getOperationNumber(), reversed.getReferenceTransactionId()));
  }

  /**
   * Builds a compensating transaction that negates the effects of the original transaction.
   *
   * @param original - the original transaction to be reversed
   * @param reason - the reason for the reversal, which will be included in the description of the compensating transaction
   * @return - a new Transaction object representing the compensating reversal
   */
  private Transaction buildCompensatingTransaction(Transaction original, String reason) {
    String formattedReason = Optional.ofNullable(reason)
            .filter(r -> !r.isBlank())
            .orElse("Compensating reversal of operation " + original.getOperationNumber());

    return Transaction.builder()
            .customerId(original.getCustomerId())
            .targetProductId(original.getTargetProductId())
            .transactionType(original.getTransactionType().getCompensatingTransactionType())
            .currency(original.getCurrency())
            .amount(original.getAmount())
            .fee(BigDecimal.ZERO)
            .description("[REVERSAL] " + formattedReason)
            .referenceTransactionId(original.getId())
            .build();
  }

  /**
   * Marks the original transaction as REVERSED and persists the change in the repository.
   *
   * @param original - the original transaction that has been reversed
   * @return - a Single emitting the updated original transaction after marking it as REVERSED
   */
  private Single<Transaction> markOriginalAsReversed(Transaction original) {
    original.setStatus(TransactionStatus.REVERSED);
    original.setUpdatedAt(Instant.now());
    return transactionRepository.save(original);
  }
}
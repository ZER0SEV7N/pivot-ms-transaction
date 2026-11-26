package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.application.support.yanki.YankiTransactionFactory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import com.pe.pivot.transaction.domain.port.in.RecordYankiTransactionUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Pure Orchestrator Use Case that records completed Yanki payments idempotently in MongoDB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordYankiTransactionUseCaseImpl implements RecordYankiTransactionUseCase {

  private final TransactionRepositoryPort transactionRepository;
  private final YankiTransactionFactory yankiTransactionFactory;

  /**
   * Records a completed Yanki payment in the transaction ledger if it has not been recorded
   * already.
   *
   * @param command - the YankiLedgerCommand containing the details of the completed payment to be
   *     recorded
   * @return - a Completable that completes when the recording is done or emits an error if the
   *     operation fails
   */
  @Override
  public Completable recordCompletedPayment(YankiLedgerCommand command) {
    // Guard clause to skip processing if the Yanki event is not completed
    if (!command.isCompleted()) {
      log.debug("Skipping non-COMPLETED Yanki event. TxId: {}, Status: {}",
              command.transactionId(), command.status());
      return Completable.complete();
    }

    // Guard clause to skip processing if the Yanki event is not successful
    String operationNumber = yankiTransactionFactory.buildOperationNumber(command.transactionId());
    return transactionRepository.findByOperationNumber(operationNumber)
            .doOnSuccess(existing -> log.info(
                    "Idempotency guard: Yanki transaction {} already recorded in ledger.",
                    operationNumber))
            // If the transaction is not found, create and save a new ledger entry
            .switchIfEmpty(Maybe.defer(() -> {
              Transaction ledgerEntry = yankiTransactionFactory.buildLedgerEntry(command);
              return transactionRepository.save(ledgerEntry)
                      .doOnSuccess(saved -> log.info(
                              "Recorded Yanki payment in ledger. OperationNumber: {}, Amount: {}",
                              saved.getOperationNumber(), saved.getAmount()))
                      .toMaybe();
            }))
            .ignoreElement();
  }
}
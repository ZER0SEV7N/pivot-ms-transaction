package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import io.reactivex.rxjava3.core.Completable;

/**
 * Input port for idempotently recording completed Yanki payments in the financial ledger.
 */
public interface RecordYankiTransactionUseCase {

  /**
   * Idempotently records a completed Yanki payment in the financial ledger based on the provided
   * command.
   *
   * @param command - the YankiLedgerCommand containing the details of the completed payment to be
   *     recorded
   * @return - a Completable that completes when the recording is successful, or emits an error if
   *     the operation fails
   */
  Completable recordCompletedPayment(YankiLedgerCommand command);
}
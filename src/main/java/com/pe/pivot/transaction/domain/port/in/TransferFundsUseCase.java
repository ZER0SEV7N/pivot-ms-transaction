package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import io.reactivex.rxjava3.core.Single;

/**
 * Input Port for executing bank transfers between accounts.
 */
public interface TransferFundsUseCase {

  Single<Transaction> transfer(TransferCommand command);
}
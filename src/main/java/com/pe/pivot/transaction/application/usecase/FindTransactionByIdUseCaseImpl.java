package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.port.in.FindTransactionByIdUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case for finding a financial transaction by its MongoDB ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindTransactionByIdUseCaseImpl implements FindTransactionByIdUseCase {

  private final TransactionRepositoryPort transactionRepository;

  /**
   * Finds a transaction by its unique identifier. If the transaction is not found, it returns an error.
   *
   * @param id - The unique identifier of the transaction to be retrieved.
   * @return - A Single emitting the found Transaction or an error if not found.
   */
  @Override
  public Single<Transaction> findById(String id) {
    log.debug("Searching transaction by ID: {}", id);
    return transactionRepository.findById(id).switchIfEmpty(Single.error(new TransactionNotFoundException("Transaction not found with ID: " + id)));
  }
}
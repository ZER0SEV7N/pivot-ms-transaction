package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.domain.exception.TransactionNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.port.in.FindTransactionByIdUseCase;
import com.pe.pivot.transaction.domain.port.in.RetrieveTransactionUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case for querying historical ledger transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveTransactionUseCaseImpl implements RetrieveTransactionUseCase {

  private final TransactionRepositoryPort transactionRepository;
  private final FindTransactionByIdUseCase findTransactionByIdUseCase;

  /**
   * Retrieves all financial transactions from the repository.
   *
   * @return - A Flowable stream of all Transaction objects.
   */
  @Override
  public Flowable<Transaction> findAll() {
    log.debug("Retrieving all financial transactions");
    return transactionRepository.findAll();
  }

  /**
   * Retrieves a financial transaction by its unique identifier.
   *
   * @param id - The unique identifier of the transaction to retrieve.
   * @return - A Single emitting the Transaction object if found, or an error if not found.
   */
  @Override
  public Single<Transaction> findById(String id) {
    return findTransactionByIdUseCase.findById(id);
  }

  /**
   * Retrieves a financial transaction by its operation number.
   *
   * @param operationNumber - The operation number of the transaction to retrieve.
   * @return - A Single emitting the Transaction object if found, or an error if not found.
   */
  @Override
  public Single<Transaction> findByOperationNumber(String operationNumber) {
    log.debug("Searching transaction by operationNumber: {}", operationNumber);
    return transactionRepository.findByOperationNumber(operationNumber)
            .switchIfEmpty(Single.error(new TransactionNotFoundException(
                    "Transaction not found with operationNumber: " + operationNumber)));
  }

  /**
   * Retrieves all financial transactions associated with a specific customer ID.
   *
   * @param customerId - The unique identifier of the customer whose transactions are to be retrieved.
   * @return - A Flowable stream of Transaction objects associated with the specified customer ID.
   */
  @Override
  public Flowable<Transaction> findByCustomerId(String customerId) {
    log.debug("Retrieving transactions for customerId: {}", customerId);
    return transactionRepository.findByCustomerId(customerId);
  }

  /**
   * Retrieves all financial transactions associated with a specific target product ID.
   *
   * @param targetProductId - The unique identifier of the target product whose transactions are to be retrieved.
   * @return - A Flowable stream of Transaction objects associated with the specified target product ID.
   */
  @Override
  public Flowable<Transaction> findByTargetProductId(String targetProductId) {
    log.debug("Retrieving transactions for targetProductId: {}", targetProductId);
    return transactionRepository.findByTargetProductId(targetProductId);
  }
}
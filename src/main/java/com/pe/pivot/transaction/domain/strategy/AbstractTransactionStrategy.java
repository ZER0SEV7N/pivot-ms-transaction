package com.pe.pivot.transaction.domain.strategy;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.CustomerNotFoundException;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base class implementing the Template Method Pattern for financial transactions.
 * Handles common customer verification and ledger persistence while delegating remote balance updates to subclasses.
 */
@RequiredArgsConstructor
public abstract class AbstractTransactionStrategy implements TransactionExecutionStrategy {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd"); // Date format for operation numbers

  protected final CustomerClientPort customerClient;
  protected final TransactionRepositoryPort transactionRepository;

  /**
   * Executes the transaction by verifying the customer, updating the ledger, and delegating the balance update to subclasses.
   *
   * @param transaction - the transaction to be executed
   * @param operationPrefix - the prefix to be used for generating the operation number
   * @param defaultFee - the default fee to be applied if applicable
   * @return - a Single emitting the executed transaction with updated details (e.g., operation number, status, etc.)
   */
  @Override
  public final Single<Transaction> execute(Transaction transaction, String operationPrefix, BigDecimal defaultFee) {
    return validateActiveCustomer(transaction.getCustomerId())
            .andThen(Single.defer(() -> executeRemoteBalanceUpdate(transaction)))
            .flatMap(snapshot -> {
              enrichCompletedTransaction(transaction, snapshot, operationPrefix, defaultFee);
              return transactionRepository.save(transaction);
            });
  }

  /**
   * Abstract method to be implemented by subclasses for updating the remote balance.
   *
   * @param transaction - the transaction for which the balance needs to be updated
   * @return - a Single emitting the updated balance snapshot
   */
  protected abstract Single<ProductBalanceSnapshot> executeRemoteBalanceUpdate(Transaction transaction);

  /**
   * Validates that the customer is active by checking with the CustomerClientPort.
   *
   * @param customerId - the ID of the customer to validate
   * @return - a Completable that completes if the customer is active, or emits an error if not
   * @throws CustomerNotFoundException if the customer does not exist
   * @throws BusinessRuleException if the customer is inactive or blocked
   */
  private Completable validateActiveCustomer(String customerId) {
    return customerClient.findCustomerById(customerId)
            .switchIfEmpty(Single.error(new CustomerNotFoundException("Customer not found in ms-customer with ID: " + customerId)))
            .flatMapCompletable(customer -> {
              if (!customer.isActive()) {
                return Completable.error(new BusinessRuleException("Cannot execute transactions for an inactive or blocked customer."));
              }
              return Completable.complete();
            });
  }

  /**
   * Enriches the transaction with details from the balance snapshot and sets its status to COMPLETED.
   *
   * @param tx - the transaction to be enriched
   * @param snapshot - the balance snapshot containing updated balance information
   * @param operationPrefix - the prefix to be used for generating the operation number
   * @param defaultFee - the default fee to be applied if applicable
   */
  private void enrichCompletedTransaction(Transaction tx, ProductBalanceSnapshot snapshot, String operationPrefix, BigDecimal defaultFee) {
    // Generate a unique operation number using the provided prefix, current date, and a random UUID segment
    String datePart = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
    String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    // Set the generated operation number and other transaction details based on the balance snapshot
    tx.setOperationNumber(operationPrefix + datePart + "-" + randomPart);
    tx.setProductCategory(getSupportedType().getProductCategory());
    tx.setMaskedProductNumber(snapshot.getMaskedProductNumber());
    tx.setCurrency(Optional.ofNullable(tx.getCurrency()).orElse(snapshot.getCurrency()));
    tx.setFee(Optional.ofNullable(tx.getFee()).orElse(defaultFee));
    tx.setPreviousBalance(snapshot.getPreviousBalance());
    tx.setNewBalance(snapshot.getNewBalance());
    tx.setStatus(TransactionStatus.COMPLETED);

    // Set the creation and update timestamps to the current instant
    Instant now = Instant.now();
    tx.setCreatedAt(now);
    tx.setUpdatedAt(now);
  }
}

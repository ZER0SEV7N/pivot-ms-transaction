package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.application.support.TransactionDomainEnricher;
import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.CustomerNotFoundException;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.domain.port.in.TransferFundsUseCase;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility Use Case orchestrating reactive bank transfers between accounts.
 * Delegates customer validation, account balance checks, and transaction enrichment to respective components.
 * Implements the TransferFundsUseCase interface, serving as the input port for executing transfers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferFundsUseCaseImpl implements TransferFundsUseCase{

  private final CustomerClientPort customerClient;
  private final AccountClientPort accountClient;
  private final TransactionRepositoryPort transactionRepository;
  private final TransactionDomainEnricher transactionEnricher;

  /**
   *  Executes a bank transfer between two accounts, ensuring that the customer is active,
   *  the source and destination accounts are different, and that the source account is debited before crediting the destination account.
   *
   * @param command - the TransferCommand containing details of the transfer, including source and destination account IDs, customer ID, and amount.
   * @return - a Single emitting the completed Transaction if successful, or an error if any business rule is violated or if the transfer fails.
   */
  @Override
  public Single<Transaction> transfer(TransferCommand command) {
    log.debug("Initiating bank transfer from account {} to account {} for amount {}",
            command.getSourceAccountId(), command.getDestinationAccountId(), command.getAmount());

    // Validate that the source and destination accounts are not the same
    if (command.getSourceAccountId().equals(command.getDestinationAccountId())) {
      return Single.error(new BusinessRuleException("Source and destination bank accounts must be different."));
    }

    // Verify that the customer is active before proceeding with the transfer
    return verifyActiveCustomer(command.getCustomerId())
            .andThen(Single.defer(() -> debitSourceAccount(command)))
            .flatMap(sourceSnapshot -> creditDestinationWithCompensation(command, sourceSnapshot))
            .doOnSuccess(tx -> log.info("Bank transfer completed. OperationNumber: {}, Fee: {}", tx.getOperationNumber(), tx.getFee()));
  }

  /**
   * Verifies that the customer associated with the transfer is active by calling the ms-customer service.
   *
   * @param customerId - the unique identifier of the customer to verify
   * @return - a Completable that completes if the customer is active, or emits an error if the customer is not found or is inactive.
   */
  private Completable verifyActiveCustomer(String customerId) {
    // Check if the customer exists and is active by calling the ms-customer service
    return customerClient.findCustomerById(customerId)
            .switchIfEmpty(Single.error(new CustomerNotFoundException("Customer not found in ms-customer with ID: " + customerId)))
            .flatMapCompletable(customer -> customer.isActive()
                    ? Completable.complete()
                    : Completable.error(new BusinessRuleException("Cannot execute transfers for an inactive or blocked customer.")));
  }

  /**
   * Debits the source account for the transfer amount by calling the ms-account service.
   *
   * @param cmd - the TransferCommand containing details of the transfer, including source account ID and amount.
   * @return - a Single emitting the ProductBalanceSnapshot of the source account after the debit operation, or an error if the debit fails.
   */
  private Single<ProductBalanceSnapshot> debitSourceAccount(TransferCommand cmd) {
    // Debit the source account by the transfer amount
    return accountClient.updateAccountBalance(
            cmd.getSourceAccountId(),
            TransactionType.TRANSFER_OUT.getRemoteOperationName(),
            cmd.getAmount()
    );
  }

  /**
   * Credits the destination account for the transfer amount by calling the ms-account service.
   *
   * @param cmd - the TransferCommand containing details of the transfer, including destination account ID and amount.
   * @param sourceSnapshot - the ProductBalanceSnapshot of the source account after the debit operation, used for creating linked transaction entries.
   * @return - a Single emitting the completed Transaction if the credit is successful, or an error if the credit fails and the source debit is compensated.
   */
  private Single<Transaction> creditDestinationWithCompensation(TransferCommand cmd, ProductBalanceSnapshot sourceSnapshot) {
    return accountClient.updateAccountBalance(
                    cmd.getDestinationAccountId(),
                    TransactionType.TRANSFER_IN.getRemoteOperationName(),
                    cmd.getAmount())
            .onErrorResumeNext(error -> compensateSourceDebit(cmd, error))
            .flatMap(destSnapshot -> persistLinkedTransferEntries(cmd, sourceSnapshot, destSnapshot));
  }

  /**
   * Compensates the source account debit by performing a deposit back to the source account if the destination credit fails.
   *
   * @param cmd - the TransferCommand containing details of the transfer, including source account ID and amount.
   * @param originalError - the Throwable representing the original error that caused the destination credit to fail.
   * @return - a Single emitting an error after the source account has been compensated, or the original error if the compensation fails.
   */
  private Single<ProductBalanceSnapshot> compensateSourceDebit(TransferCommand cmd, Throwable originalError) {
    log.warn("Destination credit failed during transfer. Executing compensating deposit on source account {}: {}",
            cmd.getSourceAccountId(), originalError.getMessage());

    // Perform a compensating deposit to the source account to reverse the debit operation
    return accountClient.updateAccountBalance(
                    cmd.getSourceAccountId(),
                    TransactionType.TRANSFER_OUT.getInverseRemoteOperationName(),
                    cmd.getAmount())
            .flatMap(ignored -> Single.error(originalError));
  }

  /**
   * Persists the linked transfer entries for both the source and destination accounts in the transaction repository.
   *
   * @param cmd - the TransferCommand containing details of the transfer, including source and destination account IDs, customer ID, and amount.
   * @param sourceSnapshot - the ProductBalanceSnapshot of the source account after the debit operation, used for creating the transfer out entry.
   * @param destSnapshot - the ProductBalanceSnapshot of the destination account after the credit operation, used for creating the transfer in entry.
   * @return - a Single emitting the saved transfer out Transaction if both entries are successfully persisted, or an error if any persistence operation fails.
   */
  private Single<Transaction> persistLinkedTransferEntries(TransferCommand cmd, ProductBalanceSnapshot sourceSnapshot, ProductBalanceSnapshot destSnapshot) {
    Transaction transferOut = transactionEnricher.buildTransferEntry(cmd, cmd.getSourceAccountId(), TransactionType.TRANSFER_OUT, sourceSnapshot, null);

    // Persist the transfer out entry first, then create and persist the transfer in entry, linking them by the transfer out ID.
    return transactionRepository.save(transferOut)
            .flatMap(savedOut -> {
              Transaction transferIn = transactionEnricher.buildTransferEntry(cmd, cmd.getDestinationAccountId(), TransactionType.TRANSFER_IN, destSnapshot, savedOut.getId());
              return transactionRepository.save(transferIn).map(ignored -> savedOut);
            });
  }
}

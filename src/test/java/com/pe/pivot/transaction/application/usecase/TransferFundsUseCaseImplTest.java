package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.application.support.TransactionDomainEnricher;
import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.CustomerNotFoundException;
import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferFundsUseCaseImplTest {

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private AccountClientPort accountClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private TransactionDomainEnricher transactionEnricher;

  private TransferFundsUseCaseImpl useCase;

  private TransferCommand command;
  private ProductBalanceSnapshot sourceSnapshot;
  private ProductBalanceSnapshot destinationSnapshot;

  @BeforeEach
  void setUp() {
    useCase = new TransferFundsUseCaseImpl(
            customerClient, accountClient, transactionRepository, transactionEnricher);
    command = TestData.transferCommand();
    sourceSnapshot = ProductBalanceSnapshot.builder()
            .productId("acc-src")
            .maskedProductNumber("****SRC")
            .currency("PEN")
            .previousBalance(new BigDecimal("500.00"))
            .newBalance(new BigDecimal("400.00"))
            .appliedCommission(new BigDecimal("0.00"))
            .build();
    destinationSnapshot = ProductBalanceSnapshot.builder()
            .productId("acc-dst")
            .maskedProductNumber("****DST")
            .currency("PEN")
            .previousBalance(new BigDecimal("100.00"))
            .newBalance(new BigDecimal("200.00"))
            .appliedCommission(new BigDecimal("0.00"))
            .build();
  }

  @Test
  @DisplayName("Should debit the source, credit the destination and persist both entries")
  void shouldTransferFunds() {
    // Arrange
    Transaction transferOut = TestData.transaction("out-1", TransactionType.TRANSFER_OUT);
    Transaction transferIn = TestData.transaction("in-1", TransactionType.TRANSFER_IN);
    stubActiveCustomer();
    when(accountClient.updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount()))
            .thenReturn(Single.just(sourceSnapshot));
    when(accountClient.updateAccountBalance("acc-dst", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.just(destinationSnapshot));
    when(transactionEnricher.buildTransferEntry(
            command, "acc-src", TransactionType.TRANSFER_OUT, sourceSnapshot, null))
            .thenReturn(transferOut);
    when(transactionEnricher.buildTransferEntry(
            command, "acc-dst", TransactionType.TRANSFER_IN, destinationSnapshot, "out-1"))
            .thenReturn(transferIn);
    when(transactionRepository.save(transferOut)).thenReturn(Single.just(transferOut));
    when(transactionRepository.save(transferIn)).thenReturn(Single.just(transferIn));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertValue(transferOut)
            .assertComplete()
            .assertNoErrors();
    verify(accountClient).updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount());
    verify(accountClient).updateAccountBalance("acc-dst", "DEPOSIT", command.getAmount());
    verify(transactionRepository).save(transferOut);
    verify(transactionRepository).save(transferIn);
  }

  @Test
  @DisplayName("Should reject a transfer between the same source and destination account")
  void shouldRejectTransferBetweenSameAccount() {
    // Arrange
    command.setDestinationAccountId(command.getSourceAccountId());

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Source and destination bank accounts must be different."))
            .assertNotComplete();
    verifyNoInteractions(customerClient, accountClient, transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should emit a customer not found error when the customer does not exist")
  void shouldEmitCustomerNotFound() {
    // Arrange
    when(customerClient.findCustomerById("cust-1")).thenReturn(Maybe.empty());

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(CustomerNotFoundException.class)
            .assertError(hasErrorMessage("Customer not found in ms-customer with ID: cust-1"))
            .assertNotComplete();
    verifyNoInteractions(accountClient, transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should reject a transfer for an inactive customer")
  void shouldRejectInactiveCustomer() {
    // Arrange
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.customer("BLOCKED")));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Cannot execute transfers for an inactive or blocked customer."))
            .assertNotComplete();
    verifyNoInteractions(accountClient, transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should propagate a source debit failure without touching the destination")
  void shouldPropagateSourceDebitFailure() {
    // Arrange
    stubActiveCustomer();
    BusinessRuleException debitError = new BusinessRuleException("Insufficient funds");
    when(accountClient.updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount()))
            .thenReturn(Single.error(debitError));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(debitError);
    verify(accountClient, never())
            .updateAccountBalance(eq("acc-dst"), anyString(), any(BigDecimal.class));
    verifyNoInteractions(transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should compensate the source debit when the destination credit fails")
  void shouldCompensateWhenDestinationCreditFails() {
    // Arrange
    stubActiveCustomer();
    BusinessRuleException creditError = new BusinessRuleException("Destination rejected");
    when(accountClient.updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount()))
            .thenReturn(Single.just(sourceSnapshot));
    when(accountClient.updateAccountBalance("acc-dst", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.error(creditError));
    when(accountClient.updateAccountBalance("acc-src", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.just(sourceSnapshot));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(creditError);
    verify(accountClient).updateAccountBalance("acc-src", "DEPOSIT", command.getAmount());
    verifyNoInteractions(transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should propagate the compensation failure when the rollback deposit fails")
  void shouldPropagateCompensationFailure() {
    // Arrange
    stubActiveCustomer();
    BusinessRuleException creditError = new BusinessRuleException("Destination rejected");
    BusinessRuleException compensationError = new BusinessRuleException("Rollback rejected");
    when(accountClient.updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount()))
            .thenReturn(Single.just(sourceSnapshot));
    when(accountClient.updateAccountBalance("acc-dst", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.error(creditError));
    when(accountClient.updateAccountBalance("acc-src", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.error(compensationError));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(compensationError);
    verifyNoInteractions(transactionRepository, transactionEnricher);
  }

  @Test
  @DisplayName("Should propagate a failure while persisting the transfer-in entry")
  void shouldPropagateTransferInPersistenceFailure() {
    // Arrange
    Transaction transferOut = TestData.transaction("out-1", TransactionType.TRANSFER_OUT);
    Transaction transferIn = TestData.transaction("in-1", TransactionType.TRANSFER_IN);
    stubActiveCustomer();
    when(accountClient.updateAccountBalance("acc-src", "WITHDRAWAL", command.getAmount()))
            .thenReturn(Single.just(sourceSnapshot));
    when(accountClient.updateAccountBalance("acc-dst", "DEPOSIT", command.getAmount()))
            .thenReturn(Single.just(destinationSnapshot));
    when(transactionEnricher.buildTransferEntry(
            command, "acc-src", TransactionType.TRANSFER_OUT, sourceSnapshot, null))
            .thenReturn(transferOut);
    when(transactionEnricher.buildTransferEntry(
            command, "acc-dst", TransactionType.TRANSFER_IN, destinationSnapshot, "out-1"))
            .thenReturn(transferIn);
    when(transactionRepository.save(transferOut)).thenReturn(Single.just(transferOut));
    when(transactionRepository.save(transferIn))
            .thenReturn(Single.error(new IllegalStateException("Database unavailable")));

    // Act & Assert
    useCase.transfer(command)
            .test()
            .assertError(IllegalStateException.class)
            .assertError(hasErrorMessage("Database unavailable"));
  }

  private void stubActiveCustomer() {
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
  }
}

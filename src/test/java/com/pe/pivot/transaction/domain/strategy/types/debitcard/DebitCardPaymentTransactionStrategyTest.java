package com.pe.pivot.transaction.domain.strategy.types.debitcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.exception.CustomerNotFoundException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.DebitCardClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DebitCardPaymentTransactionStrategy}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to execute debit card payment transactions")
class DebitCardPaymentTransactionStrategyTest {

  private static final String CUSTOMER_ID = "cust-1";
  private static final String DEBIT_CARD_ID = "dc-1";

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private DebitCardClientPort debitCardClient;

  private DebitCardPaymentTransactionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new DebitCardPaymentTransactionStrategy(
            customerClient, transactionRepository, debitCardClient);
  }

  @Test
  @DisplayName("Should be executed and persisted when the customer is active")
  void shouldBeExecutedAndPersistedWhenCustomerIsActive() {
    // (happy path)
    // Arrange
    Transaction transaction = transaction();
    ProductBalanceSnapshot snapshot = TestData.snapshot();
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(debitCardClient.payWithDebitCard(DEBIT_CARD_ID, transaction.getAmount()))
            .thenReturn(Single.just(snapshot));
    when(transactionRepository.save(transaction)).thenReturn(Single.just(transaction));

    // Act
    TestObserver<Transaction> observer = strategy.execute(transaction, "DCP-", BigDecimal.ZERO)
            .test();

    // Assert
    observer.assertNoErrors();
    observer.assertValueCount(1);
    observer.assertValue(transaction);
    observer.assertComplete();
    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    assertThat(transaction.getProductCategory()).isEqualTo(ProductCategory.DEBIT_CARD);
    assertThat(transaction.getOperationNumber()).startsWith("DCP-");
    assertThat(transaction.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(transaction.getNewBalance()).isEqualByComparingTo("600.00");
    verify(debitCardClient).payWithDebitCard(DEBIT_CARD_ID, new BigDecimal("150.00"));
    verify(transactionRepository).save(transaction);
  }

  @Test
  @DisplayName("Should be supported only for debit card payment type")
  void shouldBeSupportedOnlyForDebitCardPaymentType() {
    // Act
    TransactionType supportedType = strategy.getSupportedType();

    // Assert
    assertThat(supportedType).isEqualTo(TransactionType.DEBIT_CARD_PAYMENT);
  }

  @Test
  @DisplayName("Should be rejected when the customer does not exist")
  void shouldBeRejectedWhenCustomerDoesNotExist() {
    // (error: customer not found)
    // Arrange
    Transaction transaction = transaction();
    when(customerClient.findCustomerById(CUSTOMER_ID)).thenReturn(Maybe.empty());

    // Act
    TestObserver<Transaction> observer = strategy.execute(transaction, "DCP-", BigDecimal.ZERO)
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(CustomerNotFoundException.class);
    observer.assertNotComplete();
    verifyNoInteractions(debitCardClient);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be rejected when the customer is inactive")
  void shouldBeRejectedWhenCustomerIsInactive() {
    // (error: inactive customer)
    // Arrange
    Transaction transaction = transaction();
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(TestData.customer("INACTIVE")));

    // Act
    TestObserver<Transaction> observer = strategy.execute(transaction, "DCP-", BigDecimal.ZERO)
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    verifyNoInteractions(debitCardClient);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should be propagated when the debit card payment fails")
  void shouldBePropagatedWhenDebitCardPaymentFails() {
    // (error: remote payment rejected)
    // Arrange
    Transaction transaction = transaction();
    when(customerClient.findCustomerById(CUSTOMER_ID))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(debitCardClient.payWithDebitCard(DEBIT_CARD_ID, transaction.getAmount()))
            .thenReturn(Single.error(new BusinessRuleException("Insufficient funds")));

    // Act
    TestObserver<Transaction> observer = strategy.execute(transaction, "DCP-", BigDecimal.ZERO)
            .test();

    // Assert
    observer.assertNoValues();
    observer.assertError(BusinessRuleException.class);
    observer.assertError(error -> "Insufficient funds".equals(error.getMessage()));
    verify(transactionRepository, never()).save(any());
  }

  private Transaction transaction() {
    return Transaction.builder()
            .customerId(CUSTOMER_ID)
            .targetProductId(DEBIT_CARD_ID)
            .amount(new BigDecimal("150.00"))
            .build();
  }
}

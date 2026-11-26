package com.pe.pivot.transaction.domain.strategy.types;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.types.transaction.DepositTransactionStrategy;
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
class DepositTransactionStrategyTest {

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private AccountClientPort accountClient;

  private DepositTransactionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new DepositTransactionStrategy(customerClient, transactionRepository, accountClient);
  }

  @Test
  @DisplayName("Should support the DEPOSIT transaction type")
  void shouldSupportDepositType() {
    // Act
    TransactionType supportedType = strategy.getSupportedType();

    // Assert
    assertThat(supportedType).isEqualTo(TransactionType.DEPOSIT);
  }

  @Test
  @DisplayName("Should execute a deposit, enrich the transaction and persist it")
  void shouldExecuteDeposit() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("acc-1")
            .amount(new BigDecimal("100.00"))
            .build();
    ProductBalanceSnapshot snapshot = TestData.snapshot();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(accountClient.updateAccountBalance("acc-1", "DEPOSIT", transaction.getAmount()))
            .thenReturn(Single.just(snapshot));
    when(transactionRepository.save(transaction)).thenReturn(Single.just(transaction));

    // Act
    Transaction result = strategy.execute(transaction, "OP-", new BigDecimal("5.00"))
            .blockingGet();

    // Assert
    assertThat(result).isSameAs(transaction);
    assertThat(result.getOperationNumber()).matches("OP-\\d{8}-[A-Z0-9]{8}");
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(result.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(result.getCurrency()).isEqualTo("PEN");
    assertThat(result.getFee()).isEqualByComparingTo("1.00");
    assertThat(result.getPreviousBalance()).isEqualByComparingTo("500.00");
    assertThat(result.getNewBalance()).isEqualByComparingTo("600.00");
    assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isEqualTo(result.getCreatedAt());
    verify(accountClient).updateAccountBalance("acc-1", "DEPOSIT", transaction.getAmount());
    verify(transactionRepository).save(transaction);
  }

  @Test
  @DisplayName("Should emit a customer not found error before updating any balance")
  void shouldEmitCustomerNotFound() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("acc-1")
            .amount(new BigDecimal("100.00"))
            .build();
    when(customerClient.findCustomerById("cust-1")).thenReturn(Maybe.empty());

    // Act & Assert
    strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .test()
            .assertError(CustomerNotFoundException.class)
            .assertError(hasErrorMessage("Customer not found in ms-customer with ID: cust-1"));
    verifyNoInteractions(accountClient, transactionRepository);
  }

  @Test
  @DisplayName("Should reject a deposit for an inactive customer")
  void shouldRejectInactiveCustomer() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("acc-1")
            .amount(new BigDecimal("100.00"))
            .build();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.customer("BLOCKED")));

    // Act & Assert
    strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage(
                    "Cannot execute transactions for an inactive or blocked customer."));
    verifyNoInteractions(accountClient, transactionRepository);
  }

  @Test
  @DisplayName("Should propagate a remote balance update failure without persisting")
  void shouldPropagateRemoteFailure() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("acc-1")
            .amount(new BigDecimal("100.00"))
            .build();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(accountClient.updateAccountBalance("acc-1", "DEPOSIT", transaction.getAmount()))
            .thenReturn(Single.error(new BusinessRuleException("Balance update rejected")));

    // Act & Assert
    strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Balance update rejected"));
    verify(transactionRepository, never()).save(transaction);
  }
}

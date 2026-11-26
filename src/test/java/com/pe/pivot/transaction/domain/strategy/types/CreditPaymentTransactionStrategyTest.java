package com.pe.pivot.transaction.domain.strategy.types;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.types.credit.CreditPaymentTransactionStrategy;
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
class CreditPaymentTransactionStrategyTest {

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private CreditClientPort creditClient;

  private CreditPaymentTransactionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new CreditPaymentTransactionStrategy(
            customerClient, transactionRepository, creditClient);
  }

  @Test
  @DisplayName("Should support the CREDIT_PAYMENT transaction type")
  void shouldSupportCreditPaymentType() {
    // Act
    TransactionType supportedType = strategy.getSupportedType();

    // Assert
    assertThat(supportedType).isEqualTo(TransactionType.CREDIT_PAYMENT);
  }

  @Test
  @DisplayName("Should execute a credit payment and persist the enriched transaction")
  void shouldExecuteCreditPayment() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("cred-1")
            .amount(new BigDecimal("150.00"))
            .build();
    ProductBalanceSnapshot snapshot = TestData.snapshot();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(creditClient.updateCreditBalance("cred-1", "PAYMENT", transaction.getAmount()))
            .thenReturn(Single.just(snapshot));
    when(transactionRepository.save(transaction)).thenReturn(Single.just(transaction));

    // Act
    Transaction result = strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .blockingGet();

    // Assert
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.CREDIT_PRODUCT);
    assertThat(result.getStatus().name()).isEqualTo("COMPLETED");
    verify(creditClient).updateCreditBalance("cred-1", "PAYMENT", transaction.getAmount());
    verify(transactionRepository).save(transaction);
  }

  @Test
  @DisplayName("Should propagate a credit balance update failure without persisting")
  void shouldPropagateCreditFailure() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("cred-1")
            .amount(new BigDecimal("150.00"))
            .build();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(creditClient.updateCreditBalance("cred-1", "PAYMENT", transaction.getAmount()))
            .thenReturn(Single.error(new BusinessRuleException("Payment rejected")));

    // Act & Assert
    strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("Payment rejected"));
    verify(transactionRepository, never()).save(transaction);
  }
}

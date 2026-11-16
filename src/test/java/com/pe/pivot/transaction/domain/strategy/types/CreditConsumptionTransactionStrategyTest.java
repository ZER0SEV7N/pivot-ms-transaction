package com.pe.pivot.transaction.domain.strategy.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.CreditClientPort;
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
class CreditConsumptionTransactionStrategyTest {

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private CreditClientPort creditClient;

  private CreditConsumptionTransactionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new CreditConsumptionTransactionStrategy(
            customerClient, transactionRepository, creditClient);
  }

  @Test
  @DisplayName("Should support the CREDIT_CONSUMPTION transaction type")
  void shouldSupportCreditConsumptionType() {
    // Act
    TransactionType supportedType = strategy.getSupportedType();

    // Assert
    assertThat(supportedType).isEqualTo(TransactionType.CREDIT_CONSUMPTION);
  }

  @Test
  @DisplayName("Should execute a credit consumption and persist the enriched transaction")
  void shouldExecuteCreditConsumption() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("cred-1")
            .amount(new BigDecimal("75.00"))
            .build();
    ProductBalanceSnapshot snapshot = TestData.snapshot();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(creditClient.updateCreditBalance("cred-1", "CONSUMPTION", transaction.getAmount()))
            .thenReturn(Single.just(snapshot));
    when(transactionRepository.save(transaction)).thenReturn(Single.just(transaction));

    // Act
    Transaction result = strategy.execute(transaction, "OP-", BigDecimal.ZERO)
            .blockingGet();

    // Assert
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.CREDIT_PRODUCT);
    assertThat(result.getOperationNumber()).matches("OP-\\d{8}-[A-Z0-9]{8}");
    verify(creditClient).updateCreditBalance("cred-1", "CONSUMPTION", transaction.getAmount());
    verify(transactionRepository).save(transaction);
  }
}

package com.pe.pivot.transaction.domain.strategy.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.port.out.AccountClientPort;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.domain.strategy.types.transaction.WithdrawalTransactionStrategy;
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
class WithdrawalTransactionStrategyTest {

  @Mock
  private CustomerClientPort customerClient;

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private AccountClientPort accountClient;

  private WithdrawalTransactionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy =
            new WithdrawalTransactionStrategy(customerClient, transactionRepository, accountClient);
  }

  @Test
  @DisplayName("Should support the WITHDRAWAL transaction type")
  void shouldSupportWithdrawalType() {
    // Act
    TransactionType supportedType = strategy.getSupportedType();

    // Assert
    assertThat(supportedType).isEqualTo(TransactionType.WITHDRAWAL);
  }

  @Test
  @DisplayName("Should execute a withdrawal using the snapshot currency and the default fee")
  void shouldExecuteWithdrawalWithFallbacks() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .customerId("cust-1")
            .targetProductId("acc-1")
            .amount(new BigDecimal("50.00"))
            .build();
    ProductBalanceSnapshot snapshot = ProductBalanceSnapshot.builder()
            .maskedProductNumber("****9999")
            .currency("USD")
            .previousBalance(new BigDecimal("100.00"))
            .newBalance(new BigDecimal("50.00"))
            .appliedCommission(null)
            .build();
    when(customerClient.findCustomerById("cust-1"))
            .thenReturn(Maybe.just(TestData.activeCustomer()));
    when(accountClient.updateAccountBalance("acc-1", "WITHDRAWAL", transaction.getAmount()))
            .thenReturn(Single.just(snapshot));
    when(transactionRepository.save(transaction)).thenReturn(Single.just(transaction));

    // Act
    Transaction result = strategy.execute(transaction, "OP-", new BigDecimal("2.00"))
            .blockingGet();

    // Assert
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(result.getMaskedProductNumber()).isEqualTo("****9999");
    assertThat(result.getCurrency()).isEqualTo("USD");
    assertThat(result.getFee()).isEqualByComparingTo("2.00");
    assertThat(result.getPreviousBalance()).isEqualByComparingTo("100.00");
    assertThat(result.getNewBalance()).isEqualByComparingTo("50.00");
    verify(accountClient).updateAccountBalance("acc-1", "WITHDRAWAL", transaction.getAmount());
  }
}

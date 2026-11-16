package com.pe.pivot.transaction.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.infrastructure.config.TransactionRulesProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionDomainEnricherTest {

  @Mock
  private TransactionRulesProperties rules;

  private TransactionDomainEnricher enricher;

  @BeforeEach
  void setUp() {
    enricher = new TransactionDomainEnricher(rules);
  }

  @Test
  @DisplayName("Should build a TRANSFER_OUT entry with the command description and currency")
  void shouldBuildTransferOutEntryWithCommandDetails() {
    // Arrange
    when(rules.getOperationPrefix()).thenReturn("OP-");
    TransferCommand command = TransferCommand.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency("USD")
            .amount(new BigDecimal("150.00"))
            .description("Custom description")
            .build();
    ProductBalanceSnapshot snapshot = ProductBalanceSnapshot.builder()
            .maskedProductNumber("****1111")
            .currency("PEN")
            .previousBalance(new BigDecimal("100.00"))
            .newBalance(new BigDecimal("250.00"))
            .appliedCommission(new BigDecimal("2.50"))
            .build();

    // Act
    Transaction result = enricher.buildTransferEntry(
            command, "acc-src", TransactionType.TRANSFER_OUT, snapshot, "ref-1");

    // Assert
    assertThat(result.getOperationNumber()).matches("OP-\\d{8}-[A-Z0-9]{8}");
    assertThat(result.getCustomerId()).isEqualTo("cust-1");
    assertThat(result.getTargetProductId()).isEqualTo("acc-src");
    assertThat(result.getMaskedProductNumber()).isEqualTo("****1111");
    assertThat(result.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(result.getTransactionType()).isEqualTo(TransactionType.TRANSFER_OUT);
    assertThat(result.getCurrency()).isEqualTo("USD");
    assertThat(result.getAmount()).isEqualByComparingTo("150.00");
    assertThat(result.getFee()).isEqualByComparingTo("2.50");
    assertThat(result.getPreviousBalance()).isEqualByComparingTo("100.00");
    assertThat(result.getNewBalance()).isEqualByComparingTo("250.00");
    assertThat(result.getDescription()).isEqualTo("Custom description");
    assertThat(result.getReferenceTransactionId()).isEqualTo("ref-1");
    assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isEqualTo(result.getCreatedAt());
  }

  @Test
  @DisplayName("Should build a TRANSFER_IN entry with fallback description, currency and zero fee")
  void shouldBuildTransferInEntryWithFallbacks() {
    // Arrange
    when(rules.getOperationPrefix()).thenReturn("OP-");
    TransferCommand command = TransferCommand.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency(null)
            .amount(new BigDecimal("150.00"))
            .description(null)
            .build();
    ProductBalanceSnapshot snapshot = ProductBalanceSnapshot.builder()
            .maskedProductNumber("****1111")
            .currency("PEN")
            .previousBalance(new BigDecimal("100.00"))
            .newBalance(new BigDecimal("250.00"))
            .appliedCommission(null)
            .build();

    // Act
    Transaction result = enricher.buildTransferEntry(
            command, "acc-dst", TransactionType.TRANSFER_IN, snapshot, null);

    // Assert
    assertThat(result.getDescription())
            .isEqualTo("Bank transfer received from account acc-src");
    assertThat(result.getCurrency()).isEqualTo("PEN");
    assertThat(result.getFee()).isEqualByComparingTo("0");
    assertThat(result.getTransactionType()).isEqualTo(TransactionType.TRANSFER_IN);
  }

  @Test
  @DisplayName("Should fall back to the transfer-out description when the description is blank")
  void shouldFallbackToTransferOutDescriptionWhenBlank() {
    // Arrange
    when(rules.getOperationPrefix()).thenReturn("OP-");
    TransferCommand command = TransferCommand.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency("PEN")
            .amount(new BigDecimal("150.00"))
            .description("   ")
            .build();

    // Act
    Transaction result = enricher.buildTransferEntry(
            command, "acc-src", TransactionType.TRANSFER_OUT, TestDataSnapshot(), null);

    // Assert
    assertThat(result.getDescription()).isEqualTo("Bank transfer sent to account acc-dst");
  }

  private static ProductBalanceSnapshot TestDataSnapshot() {
    return ProductBalanceSnapshot.builder()
            .maskedProductNumber("****1111")
            .currency("PEN")
            .previousBalance(new BigDecimal("100.00"))
            .newBalance(new BigDecimal("250.00"))
            .build();
  }
}

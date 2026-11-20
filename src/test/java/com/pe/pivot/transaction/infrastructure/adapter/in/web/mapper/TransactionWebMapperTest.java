package com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.model.product.ProductReportSummary;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ProductReportResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransferRequest;
import com.pe.pivot.transaction.support.TestData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionWebMapperTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant END = Instant.parse("2026-01-31T23:59:59Z");

  private TransactionWebMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionWebMapper();
  }

  @Test
  @DisplayName("Should map a transaction request into a domain transaction")
  void shouldMapTransactionRequestToDomain() {
    // Arrange
    TransactionRequest request = TransactionRequest.builder()
            .customerId("cust-1")
            .targetProductId("prod-1")
            .transactionType(TransactionType.DEPOSIT)
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .description("Deposit")
            .build();

    // Act
    Transaction domain = mapper.toDomain(request);

    // Assert
    assertThat(domain.getCustomerId()).isEqualTo("cust-1");
    assertThat(domain.getTargetProductId()).isEqualTo("prod-1");
    assertThat(domain.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(domain.getCurrency()).isEqualTo("PEN");
    assertThat(domain.getAmount()).isEqualByComparingTo("100.00");
    assertThat(domain.getDescription()).isEqualTo("Deposit");
  }

  @Test
  @DisplayName("Should map a transfer request into a transfer command")
  void shouldMapTransferRequestToCommand() {
    // Arrange
    TransferRequest request = TransferRequest.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency("USD")
            .amount(new BigDecimal("250.00"))
            .description("Transfer")
            .build();

    // Act
    TransferCommand command = mapper.toTransferCommand(request);

    // Assert
    assertThat(command.getCustomerId()).isEqualTo("cust-1");
    assertThat(command.getSourceAccountId()).isEqualTo("acc-src");
    assertThat(command.getDestinationAccountId()).isEqualTo("acc-dst");
    assertThat(command.getCurrency()).isEqualTo("USD");
    assertThat(command.getAmount()).isEqualByComparingTo("250.00");
    assertThat(command.getDescription()).isEqualTo("Transfer");
  }

  @Test
  @DisplayName("Should map a domain transaction into a response")
  void shouldMapDomainToResponse() {
    // Arrange
    Transaction domain = TestData.transaction("tx-1", TransactionType.DEPOSIT);

    // Act
    TransactionResponse response = mapper.toResponse(domain);

    // Assert
    assertThat(response.id()).isEqualTo("tx-1");
    assertThat(response.operationNumber()).isEqualTo(domain.getOperationNumber());
    assertThat(response.customerId()).isEqualTo("cust-1");
    assertThat(response.targetProductId()).isEqualTo("prod-1");
    assertThat(response.maskedProductNumber()).isEqualTo("****1234");
    assertThat(response.productCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(response.transactionType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(response.currency()).isEqualTo("PEN");
    assertThat(response.amount()).isEqualByComparingTo("100.00");
    assertThat(response.fee()).isEqualByComparingTo("1.00");
    assertThat(response.previousBalance()).isEqualByComparingTo("500.00");
    assertThat(response.newBalance()).isEqualByComparingTo("600.00");
    assertThat(response.description()).isEqualTo("Sample transaction");
    assertThat(response.referenceTransactionId()).isNull();
    assertThat(response.status()).isEqualTo(domain.getStatus());
    assertThat(response.createdAt()).isEqualTo(domain.getCreatedAt());
    assertThat(response.updatedAt()).isEqualTo(domain.getUpdatedAt());
  }

  @Test
  @DisplayName("Should map a product report with its movements")
  void shouldMapProductReportWithMovements() {
    // Arrange
    Transaction movement = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    ProductReportSummary summary = ProductReportSummary.builder()
            .productId("prod-1")
            .maskedProductNumber("****1234")
            .productCategory(ProductCategory.BANK_ACCOUNT)
            .currency("PEN")
            .startDate(START)
            .endDate(END)
            .totalTransactions(1)
            .totalTransactedAmount(new BigDecimal("100.00"))
            .totalFeesCollected(new BigDecimal("1.00"))
            .movements(List.of(movement))
            .build();

    // Act
    ProductReportResponse response = mapper.toReportResponse(summary);

    // Assert
    assertThat(response.productId()).isEqualTo("prod-1");
    assertThat(response.maskedProductNumber()).isEqualTo("****1234");
    assertThat(response.productCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(response.currency()).isEqualTo("PEN");
    assertThat(response.startDate()).isEqualTo(START);
    assertThat(response.endDate()).isEqualTo(END);
    assertThat(response.totalTransactions()).isEqualTo(1);
    assertThat(response.totalTransactedAmount()).isEqualByComparingTo("100.00");
    assertThat(response.totalFeesCollected()).isEqualByComparingTo("1.00");
    assertThat(response.movements()).hasSize(1);
    assertThat(response.movements().get(0).id()).isEqualTo("tx-1");
  }

  @Test
  @DisplayName("Should map a product report with null movements into an empty list")
  void shouldMapProductReportWithNullMovements() {
    // Arrange
    ProductReportSummary summary = ProductReportSummary.builder()
            .productId("prod-1")
            .movements(null)
            .build();

    // Act
    ProductReportResponse response = mapper.toReportResponse(summary);

    // Assert
    assertThat(response.movements()).isNotNull().isEmpty();
  }
}

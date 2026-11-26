package com.pe.pivot.transaction.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.entity.TransactionDocument;
import com.pe.pivot.transaction.support.TestData;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionPersistenceMapperTest {

  private TransactionPersistenceMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionPersistenceMapper();
  }

  @Test
  @DisplayName("Should map a full domain transaction into a persistence document")
  void shouldMapDomainToDocument() {
    // Arrange
    Transaction domain = TestData.transaction("tx-1", TransactionType.DEPOSIT);

    // Act
    TransactionDocument document = mapper.toDocument(domain);

    // Assert
    assertThat(document.getId()).isEqualTo("tx-1");
    assertThat(document.getOperationNumber()).isEqualTo(domain.getOperationNumber());
    assertThat(document.getCustomerId()).isEqualTo("cust-1");
    assertThat(document.getTargetProductId()).isEqualTo("prod-1");
    assertThat(document.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(document.getProductCategory()).isEqualTo("BANK_ACCOUNT");
    assertThat(document.getTransactionType()).isEqualTo("DEPOSIT");
    assertThat(document.getCurrency()).isEqualTo("PEN");
    assertThat(document.getAmount()).isEqualByComparingTo("100.00");
    assertThat(document.getFee()).isEqualByComparingTo("1.00");
    assertThat(document.getPreviousBalance()).isEqualByComparingTo("500.00");
    assertThat(document.getNewBalance()).isEqualByComparingTo("600.00");
    assertThat(document.getDescription()).isEqualTo("Sample transaction");
    assertThat(document.getReferenceTransactionId()).isNull();
    assertThat(document.getStatus()).isEqualTo("COMPLETED");
    assertThat(document.getCreatedAt()).isEqualTo(domain.getCreatedAt());
    assertThat(document.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
  }

  @Test
  @DisplayName("Should map null enum values into null document fields")
  void shouldMapNullEnumsToNullDocumentFields() {
    // Arrange
    Transaction domain = Transaction.builder().id("tx-null").build();

    // Act
    TransactionDocument document = mapper.toDocument(domain);

    // Assert
    assertThat(document.getId()).isEqualTo("tx-null");
    assertThat(document.getProductCategory()).isNull();
    assertThat(document.getTransactionType()).isNull();
    assertThat(document.getStatus()).isNull();
  }

  @Test
  @DisplayName("Should map a full persistence document into a domain transaction")
  void shouldMapDocumentToDomain() {
    // Arrange
    Instant now = TestData.NOW;
    TransactionDocument document = TransactionDocument.builder()
            .id("tx-1")
            .operationNumber("OP-1")
            .customerId("cust-1")
            .targetProductId("prod-1")
            .maskedProductNumber("****1234")
            .productCategory("BANK_ACCOUNT")
            .transactionType("DEPOSIT")
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .fee(new BigDecimal("1.00"))
            .previousBalance(new BigDecimal("500.00"))
            .newBalance(new BigDecimal("600.00"))
            .description("Sample")
            .referenceTransactionId("ref-1")
            .status("COMPLETED")
            .createdAt(now)
            .updatedAt(now)
            .build();

    // Act
    Transaction domain = mapper.toDomain(document);

    // Assert
    assertThat(domain.getId()).isEqualTo("tx-1");
    assertThat(domain.getOperationNumber()).isEqualTo("OP-1");
    assertThat(domain.getCustomerId()).isEqualTo("cust-1");
    assertThat(domain.getTargetProductId()).isEqualTo("prod-1");
    assertThat(domain.getMaskedProductNumber()).isEqualTo("****1234");
    assertThat(domain.getProductCategory()).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(domain.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(domain.getCurrency()).isEqualTo("PEN");
    assertThat(domain.getAmount()).isEqualByComparingTo("100.00");
    assertThat(domain.getFee()).isEqualByComparingTo("1.00");
    assertThat(domain.getPreviousBalance()).isEqualByComparingTo("500.00");
    assertThat(domain.getNewBalance()).isEqualByComparingTo("600.00");
    assertThat(domain.getDescription()).isEqualTo("Sample");
    assertThat(domain.getReferenceTransactionId()).isEqualTo("ref-1");
    assertThat(domain.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    assertThat(domain.getCreatedAt()).isEqualTo(now);
    assertThat(domain.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should map null document enum values into null domain fields")
  void shouldMapNullDocumentEnumsToNullDomainFields() {
    // Arrange
    TransactionDocument document = TransactionDocument.builder().id("tx-null").build();

    // Act
    Transaction domain = mapper.toDomain(document);

    // Assert
    assertThat(domain.getId()).isEqualTo("tx-null");
    assertThat(domain.getProductCategory()).isNull();
    assertThat(domain.getTransactionType()).isNull();
    assertThat(domain.getStatus()).isNull();
  }
}

package com.pe.pivot.transaction.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TransactionTypeTest {

  @ParameterizedTest(name = "{0} should compensate to {1}")
  @CsvSource({
      "DEPOSIT, WITHDRAWAL",
      "WITHDRAWAL, DEPOSIT",
      "TRANSFER_IN, WITHDRAWAL",
      "TRANSFER_OUT, DEPOSIT",
      "CREDIT_PAYMENT, CREDIT_CONSUMPTION",
      "CREDIT_CONSUMPTION, CREDIT_PAYMENT"
  })
  @DisplayName("Should resolve the compensating transaction type")
  void shouldResolveCompensatingTransactionType(
      TransactionType type, TransactionType expected) {
    // Arrange

    // Act
    TransactionType compensating = type.getCompensatingTransactionType();

    // Assert
    assertThat(compensating).isEqualTo(expected);
  }

  @Test
  @DisplayName("Should expose the product category and remote operations of each type")
  void shouldExposeRoutingMetadata() {
    // Arrange
    TransactionType deposit = TransactionType.DEPOSIT;
    TransactionType consumption = TransactionType.CREDIT_CONSUMPTION;

    // Act
    ProductCategory depositCategory = deposit.getProductCategory();
    ProductCategory consumptionCategory = consumption.getProductCategory();
    String depositOperation = deposit.getRemoteOperationName();
    String depositInverse = deposit.getInverseRemoteOperationName();
    String consumptionOperation = consumption.getRemoteOperationName();
    String consumptionInverse = consumption.getInverseRemoteOperationName();

    // Assert
    assertThat(depositCategory).isEqualTo(ProductCategory.BANK_ACCOUNT);
    assertThat(consumptionCategory).isEqualTo(ProductCategory.CREDIT_PRODUCT);
    assertThat(depositOperation).isEqualTo("DEPOSIT");
    assertThat(depositInverse).isEqualTo("WITHDRAWAL");
    assertThat(consumptionOperation).isEqualTo("CONSUMPTION");
    assertThat(consumptionInverse).isEqualTo("PAYMENT");
  }
}

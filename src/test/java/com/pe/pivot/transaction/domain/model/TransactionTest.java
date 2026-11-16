package com.pe.pivot.transaction.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionTest {

  @Test
  @DisplayName("Should be reversible when the transaction is completed and has no reference")
  void shouldBeReversibleWhenCompletedWithoutReference() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .status(TransactionStatus.COMPLETED)
            .referenceTransactionId(null)
            .build();

    // Act
    boolean reversible = transaction.canBeReversed();

    // Assert
    assertThat(reversible).isTrue();
  }

  @Test
  @DisplayName("Should not be reversible when the transaction is already reversed")
  void shouldNotBeReversibleWhenAlreadyReversed() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .status(TransactionStatus.REVERSED)
            .referenceTransactionId(null)
            .build();

    // Act
    boolean reversible = transaction.canBeReversed();

    // Assert
    assertThat(reversible).isFalse();
  }

  @Test
  @DisplayName("Should not be reversible when the transaction is a compensating entry")
  void shouldNotBeReversibleWhenItIsACompensatingEntry() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .status(TransactionStatus.COMPLETED)
            .referenceTransactionId("original-tx")
            .build();

    // Act
    boolean reversible = transaction.canBeReversed();

    // Assert
    assertThat(reversible).isFalse();
  }

  @Test
  @DisplayName("Should not be reversible when the status is unknown")
  void shouldNotBeReversibleWhenStatusIsNull() {
    // Arrange
    Transaction transaction = Transaction.builder()
            .status(null)
            .referenceTransactionId(null)
            .build();

    // Act
    boolean reversible = transaction.canBeReversed();

    // Assert
    assertThat(reversible).isFalse();
  }
}

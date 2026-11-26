package com.pe.pivot.transaction.domain.model.yanki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link YankiLedgerCommand}.
 */
@DisplayName("Should be able to identify settled Yanki commands")
class YankiLedgerCommandTest {

  @Test
  @DisplayName("Should be completed when the status is COMPLETED")
  void shouldBeCompletedWhenStatusIsCompleted() {
    // (happy path)
    // Arrange
    YankiLedgerCommand command = command("COMPLETED");

    // Act
    boolean completed = command.isCompleted();

    // Assert
    assertThat(completed).isTrue();
  }

  @Test
  @DisplayName("Should be completed ignoring the status case")
  void shouldBeCompletedIgnoringTheStatusCase() {
    // Arrange
    YankiLedgerCommand command = command("completed");

    // Act
    boolean completed = command.isCompleted();

    // Assert
    assertThat(completed).isTrue();
  }

  @Test
  @DisplayName("Should not be completed when the status is FAILED")
  void shouldNotBeCompletedWhenStatusIsFailed() {
    // (error: failed payment)
    // Arrange
    YankiLedgerCommand command = command("FAILED");

    // Act
    boolean completed = command.isCompleted();

    // Assert
    assertThat(completed).isFalse();
  }

  @Test
  @DisplayName("Should not be completed when the status is null")
  void shouldNotBeCompletedWhenStatusIsNull() {
    // (error: missing status)
    // Arrange
    YankiLedgerCommand command = command(null);

    // Act
    boolean completed = command.isCompleted();

    // Assert
    assertThat(completed).isFalse();
  }

  private YankiLedgerCommand command(String status) {
    return YankiLedgerCommand.builder()
            .transactionId("tx-1")
            .status(status)
            .build();
  }
}

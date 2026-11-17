package com.pe.pivot.transaction;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for MsTransactionApplication class definition.
 */
class MsTransactionApplicationTests {

  @Test
  @DisplayName("Should instantiate MsTransactionApplication successfully")
  void shouldInstantiateApplicationSuccessfully() {
    // Arrange & Act
    MsTransactionApplication application = new MsTransactionApplication();

    // Assert
    assertNotNull(application);
  }
}

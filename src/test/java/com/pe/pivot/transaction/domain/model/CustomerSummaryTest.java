package com.pe.pivot.transaction.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerSummaryTest {

  @Test
  @DisplayName("Should report an active customer regardless of the status case")
  void shouldReportActiveCustomer() {
    // Arrange
    CustomerSummary lowerCase = TestData.customer("active");
    CustomerSummary upperCase = TestData.activeCustomer();

    // Act
    boolean lowerCaseActive = lowerCase.isActive();
    boolean upperCaseActive = upperCase.isActive();

    // Assert
    assertThat(lowerCaseActive).isTrue();
    assertThat(upperCaseActive).isTrue();
  }

  @Test
  @DisplayName("Should report an inactive customer when the status is not ACTIVE or is null")
  void shouldReportInactiveCustomer() {
    // Arrange
    CustomerSummary blocked = TestData.customer("BLOCKED");
    CustomerSummary nullStatus = TestData.customer(null);

    // Act
    boolean blockedActive = blocked.isActive();
    boolean nullStatusActive = nullStatus.isActive();

    // Assert
    assertThat(blockedActive).isFalse();
    assertThat(nullStatusActive).isFalse();
  }
}

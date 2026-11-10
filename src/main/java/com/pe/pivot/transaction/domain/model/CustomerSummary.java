package com.pe.pivot.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain projection of Customer information retrieved from ms-customer (:8081).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummary {

  private String id;
  private String customerType;
  private String documentNumber;
  private String status;

  public boolean isActive() {
    return "ACTIVE".equalsIgnoreCase(this.status);
  }
}
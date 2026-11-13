package com.pe.pivot.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Domain command object encapsulating parameters for an inter-account bank transfer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferCommand {

  private String customerId;
  private String sourceAccountId;
  private String destinationAccountId;
  private String currency;
  private BigDecimal amount;
  private String description;
}

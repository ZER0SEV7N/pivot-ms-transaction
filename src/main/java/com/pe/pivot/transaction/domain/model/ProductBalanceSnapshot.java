package com.pe.pivot.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Domain value object capturing the balance state returned by ms-account or ms-credit
 * after executing a remote balance update.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBalanceSnapshot {

  private String productId;
  private String maskedProductNumber;
  private String currency;
  private BigDecimal previousBalance;
  private BigDecimal newBalance;
}
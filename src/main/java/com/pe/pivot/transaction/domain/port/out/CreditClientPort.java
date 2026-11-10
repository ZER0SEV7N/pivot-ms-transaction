package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;

public interface CreditClientPort {
  Single<ProductBalanceSnapshot> updateCreditBalance(String creditId, String operationType, BigDecimal amount);
}
package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.ProductBalanceSnapshot;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;

public interface AccountClientPort {
  Single<ProductBalanceSnapshot> updateAccountBalance(String accountId, String operationType, BigDecimal amount);
}
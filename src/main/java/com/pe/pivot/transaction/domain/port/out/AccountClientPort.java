package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.product.ProductBalanceSnapshot;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

/**
 * Output port for updating account balances in ms-account.
 */
public interface AccountClientPort {

  Single<ProductBalanceSnapshot> updateAccountBalance(
      String accountId, String operationType, BigDecimal amount);
}

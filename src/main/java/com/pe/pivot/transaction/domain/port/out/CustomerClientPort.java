package com.pe.pivot.transaction.domain.port.out;

import com.pe.pivot.transaction.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Maybe;

public interface CustomerClientPort {
  Maybe<CustomerSummary> findCustomerById(String customerId);
}
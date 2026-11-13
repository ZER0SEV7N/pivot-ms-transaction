package com.pe.pivot.transaction.domain.port.in;

import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.time.Instant;

/**
 * Input Port for generating time-interval product reports and last 10 card movement reports.
 */
public interface GenerateTransactionReportUseCase {

  Single<ProductReportSummary> generateProductReport(String productId, Instant startDate, Instant endDate);

  Flowable<Transaction> getLastTenCardMovements(String cardId);
}
package com.pe.pivot.transaction.application.usecase;

import com.pe.pivot.transaction.application.support.TransactionReportBuilder;
import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.port.in.GenerateTransactionReportUseCase;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Single Responsibility Use Case for generating product reports and last 10 card movement reports.
 * This use case is responsible for fetching transaction data and building the appropriate reports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateTransactionReportUseCaseImpl implements GenerateTransactionReportUseCase {

  private final TransactionRepositoryPort transactionRepository;
  private final TransactionReportBuilder reportBuilder;

  /**
   * Generates a product report for a given product ID within a specified date range.
   *
   * @param productId - The unique identifier of the product for which the report is to be generated.
   * @param startDate - The start date of the reporting period (inclusive).
   * @param endDate - The end date of the reporting period (inclusive).
   * @return - Single emitting the ProductReportSummary containing aggregated transaction data for the specified product and date range.
   * @throws BusinessRuleException if the startDate is after the endDate.
   */
  @Override
  public Single<ProductReportSummary> generateProductReport(String productId, Instant startDate, Instant endDate) {
    log.debug("Generating report for productId: {} between {} and {}", productId, startDate, endDate);

    // Validate that the start date is not after the end date to ensure logical consistency in the report generation.
    if (startDate.isAfter(endDate)) {
      return Single.error(new BusinessRuleException("startDate cannot be after endDate."));
    }

    // Fetch transactions for the specified product ID and build the report using the TransactionReportBuilder.
    return transactionRepository.findByTargetProductId(productId)
            .toList()
            .map(transactions -> reportBuilder.buildProductReport(productId, startDate, endDate, transactions));
  }

  /**
   * Retrieves the last 10 movements for a given card/product ID.
   *
   * @param cardId - The unique identifier of the card/product for which the last 10 movements are to be retrieved.
   * @return - Flowable emitting the last 10 Transaction objects associated with the specified card/product ID.
   */
  @Override
  public Flowable<Transaction> getLastTenCardMovements(String cardId) {
    // Log the retrieval action for auditing and monitoring purposes.
    log.debug("Retrieving last 10 movements for card/product ID: {}", cardId);

    // Fetch transactions for the specified card/product ID and extract the last 10 movements using the TransactionReportBuilder.
    return transactionRepository.findByTargetProductId(cardId)
            .toList()
            .flatMapPublisher(transactions -> Flowable.fromIterable(reportBuilder.extractLastTenMovements(transactions)));
  }
}

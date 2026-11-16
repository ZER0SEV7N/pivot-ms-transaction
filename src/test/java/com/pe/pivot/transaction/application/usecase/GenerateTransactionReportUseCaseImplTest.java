package com.pe.pivot.transaction.application.usecase;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.application.support.TransactionReportBuilder;
import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.port.out.TransactionRepositoryPort;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateTransactionReportUseCaseImplTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant END = Instant.parse("2026-01-31T23:59:59Z");

  @Mock
  private TransactionRepositoryPort transactionRepository;

  @Mock
  private TransactionReportBuilder reportBuilder;

  private GenerateTransactionReportUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new GenerateTransactionReportUseCaseImpl(transactionRepository, reportBuilder);
  }

  @Test
  @DisplayName("Should generate a product report within the requested date range")
  void shouldGenerateProductReport() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction second = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    List<Transaction> transactions = List.of(first, second);
    ProductReportSummary summary = ProductReportSummary.builder().productId("prod-1").build();
    when(transactionRepository.findByTargetProductId("prod-1"))
            .thenReturn(Flowable.just(first, second));
    when(reportBuilder.buildProductReport("prod-1", START, END, transactions))
            .thenReturn(summary);

    // Act & Assert
    useCase.generateProductReport("prod-1", START, END)
            .test()
            .assertValue(summary)
            .assertComplete()
            .assertNoErrors();
    verify(reportBuilder).buildProductReport("prod-1", START, END, transactions);
  }

  @Test
  @DisplayName("Should reject a report when the start date is after the end date")
  void shouldRejectInvalidDateRange() {
    // Arrange
    Instant invalidStart = END.plusSeconds(1);

    // Act & Assert
    useCase.generateProductReport("prod-1", invalidStart, END)
            .test()
            .assertError(BusinessRuleException.class)
            .assertError(hasErrorMessage("startDate cannot be after endDate."))
            .assertNotComplete();
    verifyNoInteractions(transactionRepository, reportBuilder);
  }

  @Test
  @DisplayName("Should return the last ten card movements")
  void shouldReturnLastTenCardMovements() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction second = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    List<Transaction> transactions = List.of(first, second);
    when(transactionRepository.findByTargetProductId("card-1"))
            .thenReturn(Flowable.just(first, second));
    when(reportBuilder.extractLastTenMovements(transactions)).thenReturn(List.of(first));

    // Act & Assert
    useCase.getLastTenCardMovements("card-1")
            .test()
            .assertValues(first)
            .assertComplete()
            .assertNoErrors();
  }

  @Test
  @DisplayName("Should complete without movements when there are no transactions")
  void shouldCompleteEmptyWhenNoMovements() {
    // Arrange
    when(transactionRepository.findByTargetProductId("card-1")).thenReturn(Flowable.empty());
    when(reportBuilder.extractLastTenMovements(List.of())).thenReturn(List.of());

    // Act & Assert
    useCase.getLastTenCardMovements("card-1")
            .test()
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
  }
}

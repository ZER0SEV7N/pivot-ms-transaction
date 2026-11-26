package com.pe.pivot.transaction.infrastructure.adapter.in.web;

import static com.pe.pivot.transaction.support.TestAssertions.hasErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.domain.model.product.ProductReportSummary;
import com.pe.pivot.transaction.domain.port.in.GenerateTransactionReportUseCase;
import com.pe.pivot.transaction.domain.port.in.RegisterTransactionUseCase;
import com.pe.pivot.transaction.domain.port.in.RetrieveTransactionUseCase;
import com.pe.pivot.transaction.domain.port.in.ReverseTransactionUseCase;
import com.pe.pivot.transaction.domain.port.in.TransferFundsUseCase;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ProductReportResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ReversalRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransferRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper.TransactionWebMapper;
import com.pe.pivot.transaction.support.TestData;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant END = Instant.parse("2026-01-31T23:59:59Z");

  @Mock
  private RegisterTransactionUseCase registerTransactionUseCase;

  @Mock
  private ReverseTransactionUseCase reverseTransactionUseCase;

  @Mock
  private RetrieveTransactionUseCase retrieveTransactionUseCase;

  @Mock
  private TransferFundsUseCase transferFundsUseCase;

  @Mock
  private GenerateTransactionReportUseCase generateTransactionReportUseCase;

  private TransactionController controller;

  @BeforeEach
  void setUp() {
    controller = new TransactionController(
            registerTransactionUseCase,
            reverseTransactionUseCase,
            retrieveTransactionUseCase,
            transferFundsUseCase,
            generateTransactionReportUseCase,
            new TransactionWebMapper());
  }

  @Test
  @DisplayName("Should register a transaction and map the response")
  void shouldRegisterTransaction() {
    // Arrange
    TransactionRequest request = TransactionRequest.builder()
            .customerId("cust-1")
            .targetProductId("prod-1")
            .transactionType(TransactionType.DEPOSIT)
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .description("Deposit")
            .build();
    Transaction saved = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(registerTransactionUseCase.register(any(Transaction.class)))
            .thenReturn(Single.just(saved));

    // Act
    TestObserver<TransactionResponse> observer = controller.registerTransaction(request).test();

    // Assert
    observer.assertComplete().assertNoErrors();
    observer.assertValue(response -> {
      assertThat(response.id()).isEqualTo("tx-1");
      assertThat(response.transactionType()).isEqualTo(TransactionType.DEPOSIT);
      return true;
    });
    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(registerTransactionUseCase).register(captor.capture());
    assertThat(captor.getValue().getCustomerId()).isEqualTo("cust-1");
    assertThat(captor.getValue().getTargetProductId()).isEqualTo("prod-1");
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  @DisplayName("Should propagate registration errors")
  void shouldPropagateRegistrationError() {
    // Arrange
    TransactionRequest request = TransactionRequest.builder()
            .customerId("cust-1")
            .targetProductId("prod-1")
            .transactionType(TransactionType.DEPOSIT)
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .build();
    when(registerTransactionUseCase.register(any(Transaction.class)))
            .thenReturn(Single.error(new IllegalStateException("boom")));

    // Act & Assert
    controller.registerTransaction(request)
            .test()
            .assertError(IllegalStateException.class)
            .assertError(hasErrorMessage("boom"));
  }

  @Test
  @DisplayName("Should transfer funds and map the response")
  void shouldTransferFunds() {
    // Arrange
    TransferRequest request = TransferRequest.builder()
            .customerId("cust-1")
            .sourceAccountId("acc-src")
            .destinationAccountId("acc-dst")
            .currency("PEN")
            .amount(new BigDecimal("100.00"))
            .description("Transfer")
            .build();
    Transaction saved = TestData.transaction("tx-1", TransactionType.TRANSFER_OUT);
    when(transferFundsUseCase.transfer(any(TransferCommand.class))).thenReturn(Single.just(saved));

    // Act
    TestObserver<TransactionResponse> observer = controller.transferFunds(request).test();

    // Assert
    observer.assertValue(response -> "tx-1".equals(response.id())).assertComplete();
    ArgumentCaptor<TransferCommand> captor = ArgumentCaptor.forClass(TransferCommand.class);
    verify(transferFundsUseCase).transfer(captor.capture());
    assertThat(captor.getValue().getSourceAccountId()).isEqualTo("acc-src");
    assertThat(captor.getValue().getDestinationAccountId()).isEqualTo("acc-dst");
  }

  @Test
  @DisplayName("Should return every transaction")
  void shouldReturnAllTransactions() {
    // Arrange
    Transaction first = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    Transaction second = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    when(retrieveTransactionUseCase.findAll()).thenReturn(Flowable.just(first, second));

    // Act
    TestSubscriber<TransactionResponse> observer = controller.getAllTransactions().test();

    // Assert
    observer.assertComplete().assertNoErrors();
    assertThat(observer.values()).extracting(TransactionResponse::id)
            .containsExactly("tx-1", "tx-2");
  }

  @Test
  @DisplayName("Should return a transaction by ID")
  void shouldReturnTransactionById() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(retrieveTransactionUseCase.findById("tx-1")).thenReturn(Single.just(transaction));

    // Act
    TestObserver<TransactionResponse> observer = controller.getTransactionById("tx-1").test();

    // Assert
    observer.assertValue(response -> "tx-1".equals(response.id())).assertComplete();
  }

  @Test
  @DisplayName("Should return a transaction by operation number")
  void shouldReturnTransactionByOperationNumber() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(retrieveTransactionUseCase.findByOperationNumber("OP-1"))
            .thenReturn(Single.just(transaction));

    // Act
    TestObserver<TransactionResponse> observer =
            controller.getTransactionByOperationNumber("OP-1").test();

    // Assert
    observer.assertValue(response -> "OP-20260115-tx-1".equals(response.operationNumber()))
            .assertComplete();
  }

  @Test
  @DisplayName("Should return every transaction of a customer")
  void shouldReturnTransactionsByCustomerId() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(retrieveTransactionUseCase.findByCustomerId("cust-1"))
            .thenReturn(Flowable.just(transaction));

    // Act
    TestSubscriber<TransactionResponse> observer =
            controller.getTransactionsByCustomerId("cust-1").test();

    // Assert
    observer.assertValueCount(1).assertComplete().assertNoErrors();
  }

  @Test
  @DisplayName("Should return every transaction of a target product")
  void shouldReturnTransactionsByTargetProductId() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(retrieveTransactionUseCase.findByTargetProductId("prod-1"))
            .thenReturn(Flowable.just(transaction));

    // Act
    TestSubscriber<TransactionResponse> observer =
            controller.getTransactionsByTargetProductId("prod-1").test();

    // Assert
    observer.assertValueCount(1).assertComplete().assertNoErrors();
  }

  @Test
  @DisplayName("Should return a product report for the requested date range")
  void shouldReturnProductReport() {
    // Arrange
    ProductReportSummary summary = ProductReportSummary.builder()
            .productId("prod-1")
            .totalTransactions(2)
            .build();
    when(generateTransactionReportUseCase.generateProductReport("prod-1", START, END))
            .thenReturn(Single.just(summary));

    // Act
    TestObserver<ProductReportResponse> observer =
            controller.getProductReportByDateRange("prod-1", START, END).test();

    // Assert
    observer.assertValue(response -> {
      assertThat(response.productId()).isEqualTo("prod-1");
      assertThat(response.totalTransactions()).isEqualTo(2);
      return true;
    }).assertComplete();
  }

  @Test
  @DisplayName("Should return the last ten movements of a card")
  void shouldReturnLastTenCardMovements() {
    // Arrange
    Transaction transaction = TestData.transaction("tx-1", TransactionType.DEPOSIT);
    when(generateTransactionReportUseCase.getLastTenCardMovements("card-1"))
            .thenReturn(Flowable.just(transaction));

    // Act
    TestSubscriber<TransactionResponse> observer =
            controller.getLastTenCardMovements("card-1").test();

    // Assert
    observer.assertValueCount(1).assertComplete().assertNoErrors();
  }

  @Test
  @DisplayName("Should reverse a transaction using the supplied reason")
  void shouldReverseTransactionWithReason() {
    // Arrange
    Transaction reversed = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    ReversalRequest request = ReversalRequest.builder().reason("Duplicate charge").build();
    when(reverseTransactionUseCase.reverse("tx-1", "Duplicate charge"))
            .thenReturn(Single.just(reversed));

    // Act
    TestObserver<TransactionResponse> observer =
            controller.reverseTransaction("tx-1", request).test();

    // Assert
    observer.assertValue(response -> "tx-2".equals(response.id())).assertComplete();
    verify(reverseTransactionUseCase).reverse("tx-1", "Duplicate charge");
  }

  @Test
  @DisplayName("Should reverse a transaction without a request body")
  void shouldReverseTransactionWithoutBody() {
    // Arrange
    Transaction reversed = TestData.transaction("tx-2", TransactionType.WITHDRAWAL);
    when(reverseTransactionUseCase.reverse("tx-1", null)).thenReturn(Single.just(reversed));

    // Act
    TestObserver<TransactionResponse> observer = controller.reverseTransaction("tx-1", null).test();

    // Assert
    observer.assertComplete().assertNoErrors();
    verify(reverseTransactionUseCase).reverse("tx-1", null);
  }
}

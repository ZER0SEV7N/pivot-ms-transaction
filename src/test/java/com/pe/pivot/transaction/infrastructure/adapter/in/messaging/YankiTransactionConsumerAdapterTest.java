package com.pe.pivot.transaction.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import com.pe.pivot.transaction.domain.port.in.RecordYankiTransactionUseCase;
import com.pe.pivot.transaction.infrastructure.adapter.in.messaging.dto.YankiPaymentResultEvent;
import io.reactivex.rxjava3.core.Completable;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link YankiTransactionConsumerAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Should be able to consume Yanki payment result events")
class YankiTransactionConsumerAdapterTest {

  private static final Instant PROCESSED_AT = Instant.parse("2026-01-15T10:00:00Z");

  @Mock
  private RecordYankiTransactionUseCase useCase;

  private YankiTransactionConsumerAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new YankiTransactionConsumerAdapter(useCase);
  }

  @Test
  @DisplayName("Should be delegated to the use case with every mapped field")
  void shouldBeDelegatedToUseCaseWithEveryMappedField() {
    // (happy path)
    // Arrange
    YankiPaymentResultEvent event = event("COMPLETED");
    when(useCase.recordCompletedPayment(any())).thenReturn(Completable.complete());

    // Act
    adapter.onYankiPaymentResult(event);

    // Assert
    ArgumentCaptor<YankiLedgerCommand> captor = ArgumentCaptor.forClass(
            YankiLedgerCommand.class);
    verify(useCase).recordCompletedPayment(captor.capture());
    YankiLedgerCommand command = captor.getValue();
    assertThat(command.transactionId()).isEqualTo("tx-1");
    assertThat(command.status()).isEqualTo("COMPLETED");
    assertThat(command.senderPhoneNumber()).isEqualTo("987654321");
    assertThat(command.receiverPhoneNumber()).isEqualTo("999888777");
    assertThat(command.senderDebitCardId()).isEqualTo("dc-sender");
    assertThat(command.receiverDebitCardId()).isEqualTo("dc-receiver");
    assertThat(command.debitedAccountId()).isEqualTo("acc-debited");
    assertThat(command.creditedAccountId()).isEqualTo("acc-credited");
    assertThat(command.currency()).isEqualTo("PEN");
    assertThat(command.amount()).isEqualByComparingTo("30.00");
    assertThat(command.processedAt()).isEqualTo(PROCESSED_AT);
  }

  @Test
  @DisplayName("Should be swallowed when the use case fails while recording")
  void shouldBeSwallowedWhenUseCaseFailsWhileRecording() {
    // (error: ledger recording failure)
    // Arrange
    YankiPaymentResultEvent event = event("COMPLETED");
    when(useCase.recordCompletedPayment(any()))
            .thenReturn(Completable.error(new IllegalStateException("database unavailable")));

    // Act & Assert
    assertThatCode(() -> adapter.onYankiPaymentResult(event))
            .doesNotThrowAnyException();
    verify(useCase).recordCompletedPayment(any());
  }

  private YankiPaymentResultEvent event(String status) {
    return YankiPaymentResultEvent.builder()
            .transactionId("tx-1")
            .status(status)
            .senderPhoneNumber("987654321")
            .receiverPhoneNumber("999888777")
            .senderDebitCardId("dc-sender")
            .receiverDebitCardId("dc-receiver")
            .debitedAccountId("acc-debited")
            .creditedAccountId("acc-credited")
            .currency("PEN")
            .amount(new BigDecimal("30.00"))
            .failureReason(null)
            .processedAt(PROCESSED_AT)
            .build();
  }
}

package com.pe.pivot.transaction.infrastructure.adapter.in.messaging;

import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import com.pe.pivot.transaction.domain.port.in.RecordYankiTransactionUseCase;
import com.pe.pivot.transaction.infrastructure.adapter.in.messaging.dto.YankiPaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer Adapter listening to yanki.payment.result and delegating ledger recording.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YankiTransactionConsumerAdapter {

  private final RecordYankiTransactionUseCase useCase;

  /**
   * Handles incoming YankiPaymentResultEvent messages from Kafka,
   * logging the event and delegating to the use case for ledger recording.
   *
   * @param event - the YankiPaymentResultEvent received from Kafka
   */
  @KafkaListener(
          topics = "${application.kafka-topics.yanki-payment-result}",
          groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onYankiPaymentResult(YankiPaymentResultEvent event) {
    log.info("Received YankiPaymentResultEvent. TxId: {}, Status: {}",
            event.transactionId(), event.status());

    // Build a YankiLedgerCommand from the event data to pass to the use case
    YankiLedgerCommand command = YankiLedgerCommand.builder()
            .transactionId(event.transactionId())
            .status(event.status())
            .senderPhoneNumber(event.senderPhoneNumber())
            .receiverPhoneNumber(event.receiverPhoneNumber())
            .senderDebitCardId(event.senderDebitCardId())
            .receiverDebitCardId(event.receiverDebitCardId())
            .debitedAccountId(event.debitedAccountId())
            .creditedAccountId(event.creditedAccountId())
            .currency(event.currency())
            .amount(event.amount())
            .processedAt(event.processedAt())
            .build();

    // Delegate to the use case to record the completed payment in the ledger,
    // handling errors gracefully
    useCase.recordCompletedPayment(command)
            .doOnError(error -> log.error(
                    "Failed to record Yanki ledger entry for TxId {}: {}",
                    event.transactionId(), error.getMessage(), error))
            .onErrorComplete()
            .blockingAwait();
  }
}

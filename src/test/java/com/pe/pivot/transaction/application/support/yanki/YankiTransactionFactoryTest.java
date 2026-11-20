package com.pe.pivot.transaction.application.support.yanki;

import static org.assertj.core.api.Assertions.assertThat;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.domain.model.product.ProductCategory;
import com.pe.pivot.transaction.domain.model.yanki.YankiLedgerCommand;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link YankiTransactionFactory}.
 */
@DisplayName("Should be able to build Yanki ledger entries")
class YankiTransactionFactoryTest {

  private static final Instant PROCESSED_AT = Instant.parse("2026-01-15T10:00:00Z");
  private static final String SENDER_PHONE = "987654321";
  private static final String RECEIVER_PHONE = "999888777";

  private YankiTransactionFactory factory;

  @BeforeEach
  void setUp() {
    factory = new YankiTransactionFactory();
  }

  @Test
  @DisplayName("Should be built as a completed ledger entry from the command data")
  void shouldBeBuiltAsCompletedLedgerEntryFromCommandData() {
    // (happy path)
    // Arrange
    YankiLedgerCommand command = commandBuilder()
            .senderDebitCardId("dc-sender")
            .receiverDebitCardId("dc-receiver")
            .currency("USD")
            .amount(new BigDecimal("25.50"))
            .processedAt(PROCESSED_AT)
            .build();

    // Act
    Transaction ledger = factory.buildLedgerEntry(command);

    // Assert
    assertThat(ledger.getOperationNumber()).isEqualTo("YK-tx-1");
    assertThat(ledger.getCustomerId()).isEqualTo(SENDER_PHONE);
    assertThat(ledger.getTargetProductId()).isEqualTo("dc-sender");
    assertThat(ledger.getMaskedProductNumber()).isEqualTo("*****8777");
    assertThat(ledger.getProductCategory()).isEqualTo(ProductCategory.YANKI_WALLET);
    assertThat(ledger.getTransactionType()).isEqualTo(TransactionType.YANKI_PAYMENT);
    assertThat(ledger.getCurrency()).isEqualTo("USD");
    assertThat(ledger.getAmount()).isEqualByComparingTo("25.50");
    assertThat(ledger.getFee()).isEqualByComparingTo("0");
    assertThat(ledger.getPreviousBalance()).isEqualByComparingTo("0");
    assertThat(ledger.getNewBalance()).isEqualByComparingTo("0");
    assertThat(ledger.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    assertThat(ledger.getCreatedAt()).isEqualTo(PROCESSED_AT);
    assertThat(ledger.getUpdatedAt()).isEqualTo(PROCESSED_AT);
    assertThat(ledger.getDescription())
            .isEqualTo("Yanki payment from *****4321 to *****8777");
  }

  @Test
  @DisplayName("Should be defaulted to PEN and the current time when values are missing")
  void shouldBeDefaultedToPenAndCurrentTimeWhenValuesAreMissing() {
    // Arrange
    YankiLedgerCommand command = commandBuilder()
            .currency(null)
            .processedAt(null)
            .build();

    // Act
    Transaction ledger = factory.buildLedgerEntry(command);

    // Assert
    assertThat(ledger.getCurrency()).isEqualTo("PEN");
    assertThat(ledger.getCreatedAt()).isNotNull();
    assertThat(ledger.getUpdatedAt()).isEqualTo(ledger.getCreatedAt());
  }

  @Test
  @DisplayName("Should be resolved to the receiver debit card when the sender card is blank")
  void shouldBeResolvedToReceiverDebitCardWhenSenderCardIsBlank() {
    // Arrange
    YankiLedgerCommand command = commandBuilder()
            .senderDebitCardId("   ")
            .receiverDebitCardId("dc-receiver")
            .build();

    // Act
    Transaction ledger = factory.buildLedgerEntry(command);

    // Assert
    assertThat(ledger.getTargetProductId()).isEqualTo("dc-receiver");
  }

  @Test
  @DisplayName("Should be resolved to the receiver phone when no debit card is present")
  void shouldBeResolvedToReceiverPhoneWhenNoDebitCardIsPresent() {
    // (error: missing debit card identifiers)
    // Arrange
    YankiLedgerCommand command = commandBuilder()
            .senderDebitCardId(null)
            .receiverDebitCardId("")
            .build();

    // Act
    Transaction ledger = factory.buildLedgerEntry(command);

    // Assert
    assertThat(ledger.getTargetProductId()).isEqualTo(RECEIVER_PHONE);
  }

  @Test
  @DisplayName("Should be masked with the default value when the phone is too short")
  void shouldBeMaskedWithDefaultValueWhenPhoneIsTooShort() {
    // (error: invalid phone number)
    // Arrange
    YankiLedgerCommand command = commandBuilder()
            .senderPhoneNumber(null)
            .receiverPhoneNumber("123")
            .build();

    // Act
    Transaction ledger = factory.buildLedgerEntry(command);

    // Assert
    assertThat(ledger.getCustomerId()).isNull();
    assertThat(ledger.getMaskedProductNumber()).isEqualTo("****");
    assertThat(ledger.getDescription()).isEqualTo("Yanki payment from **** to ****");
  }

  @Test
  @DisplayName("Should be deterministic when building the operation number")
  void shouldBeDeterministicWhenBuildingOperationNumber() {
    // Act
    String first = factory.buildOperationNumber("tx-99");
    String second = factory.buildOperationNumber("tx-99");

    // Assert
    assertThat(first).isEqualTo("YK-tx-99");
    assertThat(second).isEqualTo(first);
  }

  private YankiLedgerCommand.YankiLedgerCommandBuilder commandBuilder() {
    return YankiLedgerCommand.builder()
            .transactionId("tx-1")
            .status("COMPLETED")
            .senderPhoneNumber(SENDER_PHONE)
            .receiverPhoneNumber(RECEIVER_PHONE)
            .amount(new BigDecimal("25.50"));
  }
}

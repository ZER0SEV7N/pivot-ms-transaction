package com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper;

import com.pe.pivot.transaction.domain.model.ProductReportSummary;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransferCommand;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ProductReportResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransferRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Web Mapper converting between immutable Record DTOs and Domain Transaction.
 */
@Component
public class TransactionWebMapper {

  /**
   * Maps a TransactionRequest DTO to a Transaction domain object.
   *
   * @param request - The TransactionRequest DTO containing the transaction details.
   * @return - A Transaction domain object constructed from the request DTO.
   */
  public Transaction toDomain(TransactionRequest request) {
    return Transaction.builder()
            .customerId(request.customerId())
            .targetProductId(request.targetProductId())
            .transactionType(request.transactionType())
            .currency(request.currency())
            .amount(request.amount())
            .description(request.description())
            .build();
  }

  /**
   * Maps a TransferRequest DTO to a TransferCommand domain object.
   *
   * @param request - The TransferRequest DTO containing the transfer details.
   * @return - A TransferCommand domain object constructed from the request DTO.
   */
  public TransferCommand toTransferCommand(TransferRequest request) {
    return TransferCommand.builder()
            .customerId(request.customerId())
            .sourceAccountId(request.sourceAccountId())
            .destinationAccountId(request.destinationAccountId())
            .currency(request.currency())
            .amount(request.amount())
            .description(request.description())
            .build();
  }

  /**
   * Maps a Transaction domain object to a TransactionResponse DTO.
   *
   * @param domain - The Transaction domain object containing the transaction details.
   * @return - A TransactionResponse DTO constructed from the domain object.
   */
  public TransactionResponse toResponse(Transaction domain) {
    return TransactionResponse.builder()
            .id(domain.getId())
            .operationNumber(domain.getOperationNumber())
            .customerId(domain.getCustomerId())
            .targetProductId(domain.getTargetProductId())
            .maskedProductNumber(domain.getMaskedProductNumber())
            .productCategory(domain.getProductCategory())
            .transactionType(domain.getTransactionType())
            .currency(domain.getCurrency())
            .amount(domain.getAmount())
            .fee(domain.getFee())
            .previousBalance(domain.getPreviousBalance())
            .newBalance(domain.getNewBalance())
            .description(domain.getDescription())
            .referenceTransactionId(domain.getReferenceTransactionId())
            .status(domain.getStatus())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
  }

  /**
   * Maps a ProductReportSummary domain object to a ProductReportResponse DTO, including mapping the list of Transaction movements.
   *
   * @param summary - The ProductReportSummary domain object containing the aggregated report data and transaction movements.
   * @return - A ProductReportResponse DTO constructed from the summary, with mapped transaction movements.
   */
  public ProductReportResponse toReportResponse(ProductReportSummary summary) {
    // Map the list of Transaction movements to TransactionResponse DTOs, handling null cases by returning an empty list.
    List<TransactionResponse> mappedMovements = summary.getMovements() == null
            ? Collections.emptyList()
            : summary.getMovements().stream().map(this::toResponse).toList();

    // Construct and return a ProductReportResponse DTO with the aggregated report data and the mapped transaction movements.
    return ProductReportResponse.builder()
            .productId(summary.getProductId())
            .maskedProductNumber(summary.getMaskedProductNumber())
            .productCategory(summary.getProductCategory())
            .currency(summary.getCurrency())
            .startDate(summary.getStartDate())
            .endDate(summary.getEndDate())
            .totalTransactions(summary.getTotalTransactions())
            .totalTransactedAmount(summary.getTotalTransactedAmount())
            .totalFeesCollected(summary.getTotalFeesCollected())
            .movements(mappedMovements)
            .build();
  }
}
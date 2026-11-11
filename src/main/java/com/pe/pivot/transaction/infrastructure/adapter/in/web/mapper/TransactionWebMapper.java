package com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper;

import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import org.springframework.stereotype.Component;

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
}
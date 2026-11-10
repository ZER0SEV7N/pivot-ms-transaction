package com.pe.pivot.transaction.infrastructure.adapter.out.persistence.mapper;

import com.pe.pivot.transaction.domain.model.ProductCategory;
import com.pe.pivot.transaction.domain.model.Transaction;
import com.pe.pivot.transaction.domain.model.TransactionStatus;
import com.pe.pivot.transaction.domain.model.TransactionType;
import com.pe.pivot.transaction.infrastructure.adapter.out.persistence.entity.TransactionDocument;
import org.springframework.stereotype.Component;

/**
 * Persistence Mapper converting between Domain Transaction and MongoDB TransactionDocument.
 */
@Component
public class TransactionPersistenceMapper {

  /**
   * Maps a domain Transaction object to a persistence TransactionDocument for MongoDB storage.
   *
   * @param domain - The domain Transaction object to be converted.
   * @return - A TransactionDocument suitable for persistence in MongoDB.
   */
  public TransactionDocument toDocument(Transaction domain) {
    return TransactionDocument.builder()
            .id(domain.getId())
            .operationNumber(domain.getOperationNumber())
            .customerId(domain.getCustomerId())
            .targetProductId(domain.getTargetProductId())
            .maskedProductNumber(domain.getMaskedProductNumber())
            .productCategory(domain.getProductCategory() != null ? domain.getProductCategory().name() : null)
            .transactionType(domain.getTransactionType() != null ? domain.getTransactionType().name() : null)
            .currency(domain.getCurrency())
            .amount(domain.getAmount())
            .fee(domain.getFee())
            .previousBalance(domain.getPreviousBalance())
            .newBalance(domain.getNewBalance())
            .description(domain.getDescription())
            .referenceTransactionId(domain.getReferenceTransactionId())
            .status(domain.getStatus() != null ? domain.getStatus().name() : null)
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
  }

  /**
   * Maps a persistence TransactionDocument from MongoDB to a domain Transaction object.
   *
   * @param doc - The TransactionDocument retrieved from MongoDB to be converted.
   * @return - A Transaction object suitable for use in the domain layer.
   */
  public Transaction toDomain(TransactionDocument doc) {
    return Transaction.builder()
            .id(doc.getId())
            .operationNumber(doc.getOperationNumber())
            .customerId(doc.getCustomerId())
            .targetProductId(doc.getTargetProductId())
            .maskedProductNumber(doc.getMaskedProductNumber())
            .productCategory(doc.getProductCategory() != null ? ProductCategory.valueOf(doc.getProductCategory()) : null)
            .transactionType(doc.getTransactionType() != null ? TransactionType.valueOf(doc.getTransactionType()) : null)
            .currency(doc.getCurrency())
            .amount(doc.getAmount())
            .fee(doc.getFee())
            .previousBalance(doc.getPreviousBalance())
            .newBalance(doc.getNewBalance())
            .description(doc.getDescription())
            .referenceTransactionId(doc.getReferenceTransactionId())
            .status(doc.getStatus() != null ? TransactionStatus.valueOf(doc.getStatus()) : null)
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
  }
}
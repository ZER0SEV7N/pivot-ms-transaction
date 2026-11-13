package com.pe.pivot.transaction.infrastructure.adapter.in.web;

import com.pe.pivot.transaction.domain.port.in.*;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ProductReportResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ReversalRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransferRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper.TransactionWebMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST Controller implementing the OpenAPI contract for the Immutable Financial Ledger with RxJava 3.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final RegisterTransactionUseCase registerTransactionUseCase;
  private final ReverseTransactionUseCase reverseTransactionUseCase;
  private final RetrieveTransactionUseCase retrieveTransactionUseCase;
  private final TransferFundsUseCase transferFundsUseCase;
  private final GenerateTransactionReportUseCase generateTransactionReportUseCase;
  private final TransactionWebMapper mapper;

  /**
   * Register and execute a new financial transaction.
   * @POST /api/v1/transactions
   * @param request - The TransactionRequest containing details of the transaction to be registered and executed.
   * @body : {
   *  - "customerId": "12345",
   *  - "targetProductId": "67890",
   *  - "transactionType": "DEBIT",
   *  - "amount": 100.00,
   *  - "currency": "USD",
   *  - "description": "Payment for invoice #456"
   * }
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<TransactionResponse> registerTransaction(@Valid @RequestBody TransactionRequest request) {
    return registerTransactionUseCase.register(mapper.toDomain(request))
            .map(mapper::toResponse);
  }

  /**
   * Transfer funds between two accounts.
   *
   * @param request - The TransferRequest containing details of the transfer, including source and destination accounts, amount, and currency.
   * @return - A Single emitting the TransactionResponse for the transferred funds, or an error if the transfer fails.
   */
  @PostMapping("/transfer")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<TransactionResponse> transferFunds(@Valid @RequestBody TransferRequest request) {
    return transferFundsUseCase.transfer(mapper.toTransferCommand(request))
            .map(mapper::toResponse);
  }

  /**
   * Retrieve all financial transactions.
   * @GET /api/v1/transactions
   * @return - A Flowable emitting TransactionResponse objects for each transaction in the ledger,
   * or an empty Flowable if no transactions exist.
   */
  @GetMapping
  public Flowable<TransactionResponse> getAllTransactions() {
    return retrieveTransactionUseCase.findAll()
            .map(mapper::toResponse);
  }

  /**
   * Retrieve a transaction by its unique ID.
   * @GET /api/v1/transactions/{id} : Retrieve a transaction by its unique ID.
   * @param id - The unique identifier of the transaction to be retrieved.
   * @return - A Single emitting the TransactionResponse for the specified ID,
   * or an error if no transaction is found with that ID.
   */
  @GetMapping("/{id}")
  public Single<TransactionResponse> getTransactionById(@PathVariable String id) {
    return retrieveTransactionUseCase.findById(id)
            .map(mapper::toResponse);
  }

  /**
   * Retrieve a transaction by its operation number.
   * @GET /api/v1/transactions/operation/{operationNumber}
   * @param operationNumber - The unique operation number of the transaction to be retrieved.
   * @return - A Single emitting the TransactionResponse for the specified operation number,
   * or an error if no transaction is found with that operation number.
   */
  @GetMapping("/operation/{operationNumber}")
  public Single<TransactionResponse> getTransactionByOperationNumber(@PathVariable String operationNumber) {
    return retrieveTransactionUseCase.findByOperationNumber(operationNumber)
            .map(mapper::toResponse);
  }

  /**
   * Retrieve all transactions executed by a customer.
   * @GET /api/v1/transactions/customer/{customerId}
   * @param customerId - The unique identifier of the customer for whom transactions are to be retrieved.
   * @return - A Flowable emitting TransactionResponse objects for each transaction associated with the specified customer ID,
   * or an empty Flowable if no transactions are found.
   */
  @GetMapping("/customer/{customerId}")
  public Flowable<TransactionResponse> getTransactionsByCustomerId(@PathVariable String customerId) {
    return retrieveTransactionUseCase.findByCustomerId(customerId)
            .map(mapper::toResponse);
  }

  /**
   * Retrieve all transactions for a specific Account or Credit ID.
   * @GET /api/v1/transactions/product/{targetProductId}
   * @param targetProductId - The unique identifier of the target product (Account or Credit) for which transactions are to be retrieved.
   * @return - A Flowable emitting TransactionResponse objects for each transaction associated with the specified target product ID,
   * or an empty Flowable if no transactions are found.
   */
  @GetMapping("/product/{targetProductId}")
  public Flowable<TransactionResponse> getTransactionsByTargetProductId(@PathVariable String targetProductId) {
    return retrieveTransactionUseCase.findByTargetProductId(targetProductId)
            .map(mapper::toResponse);
  }

  /**
   * Generate a product report for a specific product ID within a given date range.
   * @GET /api/v1/transactions/reports/product/{productId}?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z
   * @param productId - The unique identifier of the product for which the report is to be generated.
   * @param startDate - The start date of the reporting period (inclusive) in ISO-8601 format (e.g., 2024-01-01T00:00:00Z).
   * @param endDate - The end date of the reporting period (inclusive) in ISO-8601 format (e.g., 2024-01-31T23:59:59Z).
   * @return - A Single emitting the ProductReportResponse containing aggregated transaction data for the specified product and date range,
   */
  @GetMapping("/reports/product/{productId}")
  public Single<ProductReportResponse> getProductReportByDateRange(
          @PathVariable String productId,
          @RequestParam Instant startDate,
          @RequestParam Instant endDate) {
    return generateTransactionReportUseCase.generateProductReport(productId, startDate, endDate)
            .map(mapper::toReportResponse);
  }

  /**
   * Retrieve the last 10 movements for a specific card/product ID.
   * @GET /api/v1/transactions/reports/card/{cardId}/last-movements
   * @param cardId - The unique identifier of the card/product for which the last 10 movements are to be retrieved.
   * @return - A Flowable emitting TransactionResponse objects for the last 10 transactions associated with the specified card/product ID,
   * or an empty Flowable if no transactions are found.
   */
  @GetMapping("/reports/card/{cardId}/last-movements")
  public Flowable<TransactionResponse> getLastTenCardMovements(@PathVariable String cardId) {
    return generateTransactionReportUseCase.getLastTenCardMovements(cardId)
            .map(mapper::toResponse);
  }

  /**
   * Execute a compensating reversal for a COMPLETED transaction.
   *
   * @POST /api/v1/transactions/{id}/reverse
   * @param id - The unique identifier of the transaction to be reversed.
   * @param request - Optional request body containing the reason for the reversal.
   * @body : {
   *   "reason": "Customer requested reversal due to duplicate charge."
   * }
   * @return - A Single emitting the TransactionResponse of the reversed transaction, or an error if the reversal fails.
   *
   */
  @PostMapping("/{id}/reverse")
  public Single<TransactionResponse> reverseTransaction(@PathVariable String id, @RequestBody(required = false) ReversalRequest request) {
    String reason = request != null ? request.reason() : null;
    return reverseTransactionUseCase.reverse(id, reason)
            .map(mapper::toResponse);
  }
}
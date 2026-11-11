package com.pe.pivot.transaction.infrastructure.adapter.in.web;

import com.pe.pivot.transaction.domain.port.in.RegisterTransactionUseCase;
import com.pe.pivot.transaction.domain.port.in.RetrieveTransactionUseCase;
import com.pe.pivot.transaction.domain.port.in.ReverseTransactionUseCase;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.ReversalRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.pe.pivot.transaction.infrastructure.adapter.in.web.mapper.TransactionWebMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
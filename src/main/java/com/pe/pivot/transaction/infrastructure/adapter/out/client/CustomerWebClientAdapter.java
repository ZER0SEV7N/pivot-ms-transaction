package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import com.pe.pivot.transaction.domain.exception.BusinessRuleException;
import com.pe.pivot.transaction.domain.model.CustomerSummary;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.CustomerClientResponse;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

/**
 * Output Adapter communicating reactively with ms-customer (:8081)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerWebClientAdapter implements CustomerClientPort {

  private final WebClient customerWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  /**
   * Finds a customer by ID using the ms-customer service. If the customer is not found or an error occurs, it returns an empty Maybe.
   *
   * @param customerId - The ID of the customer to be retrieved.
   * @return - A Maybe containing the CustomerSummary if found, or empty if not found or an error occurs.
   */
  @Override
  public Maybe<CustomerSummary> findCustomerById(String customerId) {
    log.debug("Calling ms-customer GET /{} for customerId: {}", customerId, customerId);

    // Use WebClient to make a GET request to the ms-customer service, handling 4xx errors gracefully and applying a circuit breaker for resilience.
    Mono<CustomerSummary> mono = customerWebClient.get()
            .uri("/{id}", customerId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
            .bodyToMono(CustomerClientResponse.class)
            .map(this::toDomain)

            // Apply a circuit breaker to handle potential failures or timeouts when calling the ms-customer service.
            .transform(it -> circuitBreakerFactory.create("customerServiceCircuitBreaker")
                    .run(it, throwable -> {
                      log.error("Circuit breaker triggered calling ms-customer for customerId {}: {}", customerId, throwable.getMessage());
                      return Mono.error(new BusinessRuleException("Customer service (ms-customer) is currently unavailable or timed out (2s)."));
                    }));

    return RxJava3Adapter.monoToMaybe(mono);
  }

  /**
   * Maps the CustomerClientResponse DTO to the CustomerSummary domain model.
   *
   * @param dto - The CustomerClientResponse received from the ms-customer service.
   * @return - The CustomerSummary domain model.
   */
  private CustomerSummary toDomain(CustomerClientResponse dto) {
    return CustomerSummary.builder()
            .id(dto.id())
            .customerType(dto.customerType())
            .documentNumber(dto.documentNumber())
            .status(dto.status())
            .build();
  }
}

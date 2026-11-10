package com.pe.pivot.transaction.infrastructure.adapter.out.client;

import com.pe.pivot.transaction.domain.model.CustomerSummary;
import com.pe.pivot.transaction.domain.port.out.CustomerClientPort;
import com.pe.pivot.transaction.infrastructure.adapter.out.client.dto.CustomerClientResponse;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  /**
   * Finds a customer by ID using the ms-customer service. If the customer is not found or an error occurs, it returns an empty Maybe.
   *
   * @param customerId - The ID of the customer to be retrieved.
   * @return - A Maybe containing the CustomerSummary if found, or empty if not found or an error occurs.
   */
  @Override
  public Maybe<CustomerSummary> findCustomerById(String customerId) {
    log.debug("Calling ms-customer GET /{} for customerId: {}", customerId, customerId);

    // Create a Mono that performs the GET request to ms-customer and maps the response to CustomerSummary
    Mono<CustomerSummary> mono = customerWebClient.get()
            .uri("/{id}", customerId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
            .bodyToMono(CustomerClientResponse.class)
            .map(this::toDomain)
            .onErrorResume(ex -> {
              log.warn("Customer lookup returned empty or error for ID {}: {}", customerId, ex.getMessage());
              return Mono.empty();
            });

    return RxJava3Adapter.monoToMaybe(mono);
  }

  private CustomerSummary toDomain(CustomerClientResponse dto) {
    return CustomerSummary.builder()
            .id(dto.id())
            .customerType(dto.customerType())
            .documentNumber(dto.documentNumber())
            .status(dto.status())
            .build();
  }
}

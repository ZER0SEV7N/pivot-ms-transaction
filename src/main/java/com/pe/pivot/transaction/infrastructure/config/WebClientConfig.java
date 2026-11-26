package com.pe.pivot.transaction.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration defining reactive WebClient beans for ms-customer (:8081),
 * ms-account (:8082), and ms-credit (:8083).
 */
@Configuration
public class WebClientConfig {

  @Value("${application.clients.customer-service.url:http://localhost:8081/api/v1/customers}")
  private String customerServiceUrl;

  @Value("${application.clients.account-service.url:http://localhost:8082/api/v1/accounts}")
  private String accountServiceUrl;

  @Value("${application.clients.credit-service.url:http://localhost:8083/api/v1/credits}")
  private String creditServiceUrl;

  @Value("${application.clients.debit-card-service.url:http://localhost:8082/api/v1/debit-cards}")
  private String debitCardServiceUrl;

  /**
   * Creates the WebClient used to communicate with ms-customer.
   *
   * @return a configured WebClient for ms-customer
   */
  @Bean
  public WebClient customerWebClient() {
    return WebClient.builder()
            .baseUrl(customerServiceUrl)
            .build();
  }

  /**
   * Creates the WebClient used to communicate with ms-account.
   *
   * @return a configured WebClient for ms-account
   */
  @Bean
  public WebClient accountWebClient() {
    return WebClient.builder()
            .baseUrl(accountServiceUrl)
            .build();
  }

  /**
   * Creates the WebClient used to communicate with ms-credit.
   *
   * @return a configured WebClient for ms-credit
   */
  @Bean
  public WebClient creditWebClient() {
    return WebClient.builder()
            .baseUrl(creditServiceUrl)
            .build();
  }

  /**
   * Creates the WebClient used to communicate with ms-debit-card.
   *
   * @return a configured WebClient for ms-debit-card
   */
  @Bean
  public WebClient debitCardWebClient() {
    return WebClient.builder().baseUrl(debitCardServiceUrl).build();
  }
}

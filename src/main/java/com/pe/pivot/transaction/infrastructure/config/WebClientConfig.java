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

  @Bean
  public WebClient customerWebClient() {
    return WebClient.builder()
            .baseUrl(customerServiceUrl)
            .build();
  }

  @Bean
  public WebClient accountWebClient() {
    return WebClient.builder()
            .baseUrl(accountServiceUrl)
            .build();
  }

  @Bean
  public WebClient creditWebClient() {
    return WebClient.builder()
            .baseUrl(creditServiceUrl)
            .build();
  }
}
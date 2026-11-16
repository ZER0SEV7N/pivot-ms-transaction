package com.pe.pivot.transaction.infrastructure.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration properties for transaction rules fetched from ms-config.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.transaction-rules")
public class TransactionRulesProperties {

  private String operationPrefix = "OP-";
  private BigDecimal defaultFee = BigDecimal.ZERO;
}

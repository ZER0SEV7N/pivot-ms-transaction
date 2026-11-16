package com.pe.pivot.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.config.import="
})
class MsTransactionApplicationTests {

  @Test
  @DisplayName("Should load the Spring application context")
  void contextLoads() {
  }

}

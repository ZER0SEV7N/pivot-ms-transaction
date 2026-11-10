package com.pe.pivot.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Immutable Financial Ledger Microservice (pivot-ms-transaction).
 */
@SpringBootApplication
public class MsTransactionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsTransactionApplication.class, args);
	}
}
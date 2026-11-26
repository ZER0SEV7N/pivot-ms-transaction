# Pivot Banking System - Transaction Microservice (`pivot-ms-transaction`)

## Overview
Reactive microservice (`:8084`) managing the **Immutable Financial Ledger**. It orchestrates account **Deposits** (`DEPOSIT`), **Withdrawals** (`WITHDRAWAL`), **Credit Payments** (`CREDIT_PAYMENT`), **Third-Party Credit Payments** (`THIRD_PARTY_CREDIT_PAYMENT`), **Credit Card Consumptions** (`CREDIT_CONSUMPTION`), **Debit Card Payments & Withdrawals** (`DEBIT_CARD_PAYMENT`, `DEBIT_CARD_WITHDRAWAL`), **Inter-Account Bank Transfers** (`TRANSFER_OUT` / `TRANSFER_IN`) with compensating rollbacks, **Compensating Reversals**, **Time-Interval Product Reports**, **Last 10 Card Movements Reports** (`Java Streams`), and **idempotent Kafka ledger recording of Yanki mobile wallet payments** (`YANKI_PAYMENT`, Phase III).

## Architecture, Design Patterns & Quality Gates
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) separating `domain`, `application`, and `infrastructure` layers, developed **API-First** from `transaction-api.yaml`.
- **Reactive Stack:** Java 17 + Spring Boot `4.1.2` + Spring Cloud `2025.1.4` + Spring WebFlux (Netty) + **RxJava 3 (`3.1.12`)** (`Single`, `Maybe`, `Flowable`, `Completable`) + `reactor-adapter` + Reactive Spring Data MongoDB + **Spring Boot Starter Kafka** + Netflix Eureka Client + **Resilience4j**.
- **SOLID & Design Patterns (Zero God Classes):**
  - **Single Responsibility Principle (SRP):** Dedicated reactive use cases (`RegisterTransactionUseCaseImpl`, `TransferFundsUseCaseImpl`, `GenerateTransactionReportUseCaseImpl`, `ReverseTransactionUseCaseImpl`, `RetrieveTransactionUseCaseImpl`, `FindTransactionByIdUseCaseImpl`, and `RecordYankiTransactionUseCaseImpl`).
  - **Template Method + Strategy + Registry Pattern (OCP):** `AbstractTransactionStrategy`, `TransactionStrategyRegistry` (O(1) lookup via `EnumMap`), and concrete strategies registered in `StrategyConfig`:
    - Phase I & II: `DepositTransactionStrategy`, `WithdrawalTransactionStrategy`, `CreditPaymentTransactionStrategy`, `CreditConsumptionTransactionStrategy`.
    - Phase III: `ThirdPartyCreditPaymentStrategy`, `DebitCardPaymentStrategy`, and `DebitCardWithdrawalStrategy`.
  - **Domain Support (`application/support/`):** `TransactionDomainEnricher`, `TransactionReportBuilder` (`Java Streams`: `filter`, `sorted`, `limit(10)`, `map`, `reduce`), and `YankiTransactionFactory` (Phase III).
  - **Compensating Saga Pattern:** `TransferFundsUseCaseImpl` automatically refunds the source account if crediting the destination account fails or violates currency consistency (`PEN` vs `USD`).
  - **Idempotent Kafka Consumer (Phase III):** Consumes `yanki.payment.result` events and records `YANKI_PAYMENT` ledger entries under `ProductCategory.YANKI_WALLET` without duplicating already-recorded transactions.
  - **Circuit Breaker Pattern (`Resilience4j`):** Protects outbound calls to `ms-customer`, `ms-account` (accounts and `DebitCardWebClientAdapter`), and `ms-credit` with a **2-second timeout**.
- **Code Quality & Testing:**
  - **Google Java Style (`maven-checkstyle-plugin`):** Enforced during Maven `validate` phase.
  - **Unit Testing (`JUnit 5` + `Mockito` + `RxJava 3 TestObserver` + `StepVerifier`):** AAA pattern and FIRST principles achieving **~99.7% instruction coverage** verified by **JaCoCo (`0.8.15`)** (minimum gate: 90%).

## API Endpoints (`/api/v1/transactions`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/transactions` | Execute `DEPOSIT`, `WITHDRAWAL`, `CREDIT_PAYMENT`, `THIRD_PARTY_CREDIT_PAYMENT`, `CREDIT_CONSUMPTION`, `DEBIT_CARD_PAYMENT`, or `DEBIT_CARD_WITHDRAWAL` |
| `POST` | `/api/v1/transactions/transfer` | Execute a bank transfer between accounts (same customer or third-party) with compensating rollback |
| `GET` | `/api/v1/transactions` | Retrieve all ledger transactions (including `YANKI_PAYMENT` entries) |
| `GET` | `/api/v1/transactions/{id}` | Retrieve transaction by ID |
| `GET` | `/api/v1/transactions/operation/{operationNumber}` | Retrieve transaction by unique operation number (`OP-YYYYMMDD-XXXXXXXX`) |
| `GET` | `/api/v1/transactions/customer/{customerId}` | Retrieve all transactions executed by a customer |
| `GET` | `/api/v1/transactions/product/{targetProductId}` | Retrieve all movements of a specific bank account, credit product, debit card, or Yanki wallet |
| `GET` | `/api/v1/transactions/reports/product/{productId}` | Generate a general product report within `[startDate, endDate]` (`Java Streams`) |
| `GET` | `/api/v1/transactions/reports/card/{cardId}/last-movements` | Retrieve the **last 10 movements** of a debit or credit card (`Java Streams`) |
| `POST` | `/api/v1/transactions/{id}/reverse` | Execute a compensating ledger reversal (marks original as `REVERSED`, requires `ROLE_ADMIN`) |

## Inter-Service Communication, Kafka & Observability
- **`pivot-ms-config` (`:8888`):** Fetches externalized rules (`TransactionRulesProperties`) and Kafka settings.
- **`pivot-ms-eureka` (`:8761`):** Registers service instance as `MS-TRANSACTION`.
- **`pivot-ms-customer` (`:8081`):** Verifies active customer status via `WebClient` + `Resilience4j` (2s timeout).
- **`pivot-ms-account` (`:8082`):** Updates account balances and executes cascading debit card charges via `DebitCardWebClientAdapter` + `Resilience4j` (2s timeout).
- **`pivot-ms-credit` (`:8083`):** Updates credit balances (own and third-party payments, credit card consumptions) via `WebClient` + `Resilience4j` (2s timeout).
- **Apache Kafka (`:9092`):** Consumes `yanki.payment.result` asynchronously to persist `COMPLETED` Yanki payments into the financial ledger.
- **Observability:** Exposes `/actuator/health/liveness` & `/actuator/health/readiness` and ships structured logs via **Promtail -> Loki -> Grafana (`:3000`)**.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7.0 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`, configured via `spring.mongodb.uri`).
- **Database Name:** `db_pivot_transaction`
- **Collection:** `transactions`

## How to Run (Local, Docker & Kubernetes)
1. Ensure MongoDB (`:27017`), Kafka (`:9092`), `ms-config` (`:8888`), `ms-eureka` (`:8761`), `ms-customer` (`:8081`), `ms-account` (`:8082`), and `ms-credit` (`:8083`) are running.
2. Run unit tests, Checkstyle validation, and verify the 90% JaCoCo coverage gate:
   ```powershell
   mvn clean verify
   ```
3. Start the microservice locally (port `8084`):
   ```powershell
   mvn spring-boot:run
   ```
4. Build and run as a Docker container (or deploy via `ENVIROMENT/k8s/05-microservices.yaml` in namespace `pivot-banking`):
   ```powershell
   docker build -t pivot/ms-transaction:1.0.0 .
   docker run -d -p 8084:8084 --name pivot-ms-transaction pivot/ms-transaction:1.0.0
   ```

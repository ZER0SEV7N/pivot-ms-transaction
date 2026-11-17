# Pivot Banking System - Transaction Microservice (`pivot-ms-transaction`)

## Overview
Reactive microservice managing the **Immutable Financial Ledger**. It orchestrates account **Deposits** (`DEPOSIT`), **Withdrawals** (`WITHDRAWAL`), **Credit Payments** (`CREDIT_PAYMENT`), **Credit Card Consumptions** (`CREDIT_CONSUMPTION`), **Inter-Account Bank Transfers** (`TRANSFER_OUT` / `TRANSFER_IN`) with compensating rollbacks, **Compensating Reversals**, **Time-Interval Product Reports**, and **Last 10 Card Movements Reports** using **Java Streams** and **RxJava 3**.

## Architecture, Design Patterns & Quality Gates
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) separating `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Java 17 + Spring Boot 4.1.2 + Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`) + Netflix Eureka Client + **Resilience4j**.
- **SOLID & Design Patterns (Zero God Classes):**
  - **Single Responsibility Principle (SRP):** Dedicated reactive use cases (`RegisterTransactionUseCaseImpl`, `TransferFundsUseCaseImpl`, `GenerateTransactionReportUseCaseImpl`, `ReverseTransactionUseCaseImpl`, `RetrieveTransactionUseCaseImpl`, `FindTransactionByIdUseCaseImpl`).
  - **Template Method + Strategy + Registry Pattern:** `AbstractTransactionStrategy`, `TransactionStrategyRegistry` ($O(1)$ lookup via `EnumMap`), and concrete strategies in `domain/strategy/types/` (`DepositTransactionStrategy`, `WithdrawalTransactionStrategy`, `CreditPaymentTransactionStrategy`, `CreditConsumptionTransactionStrategy`).
  - **Domain Enricher & Stream Report Builder (`application/support/`):**
    - `TransactionDomainEnricher`: Constructs linked `TRANSFER_OUT` and `TRANSFER_IN` ledger entries capturing automatic commissions (`appliedCommission`) charged by `ms-account`.
    - `TransactionReportBuilder`: Leverages **Java Streams** (`filter`, `sorted`, `limit(10)`, `map`, `reduce`) to aggregate date-range product reports and the 10 most recent card movements.
  - **Compensating Saga Pattern:** `TransferFundsUseCaseImpl` automatically refunds the source account if crediting the destination account fails or violates currency consistency (`PEN` vs `USD`).
  - **Circuit Breaker Pattern (`Resilience4j`):** Protects outbound calls to `ms-customer`, `ms-account`, and `ms-credit` with a strict **2-second timeout**.
- **Code Quality & Testing:**
  - **Google Java Style (`maven-checkstyle-plugin`):** Enforced during Maven `validate` phase.
  - **Unit Testing (`JUnit 5` + `Mockito` + `RxJava 3 TestObserver` + `StepVerifier`):** AAA pattern and FIRST principles achieving **>90% code coverage** in **JaCoCo** and **SonarQube**.

## API Endpoints (`/api/v1/transactions`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/transactions` | Execute a `DEPOSIT`, `WITHDRAWAL`, `CREDIT_PAYMENT`, or `CREDIT_CONSUMPTION` |
| `POST` | `/api/v1/transactions/transfer` | Execute a bank transfer between accounts (same customer or third-party) |
| `GET` | `/api/v1/transactions` | Retrieve all ledger transactions |
| `GET` | `/api/v1/transactions/{id}` | Retrieve transaction by ID |
| `GET` | `/api/v1/transactions/operation/{operationNumber}` | Retrieve transaction by unique operation number (`OP-YYYYMMDD-XXXXXXXX`) |
| `GET` | `/api/v1/transactions/customer/{customerId}` | Retrieve all transactions executed by a customer |
| `GET` | `/api/v1/transactions/product/{targetProductId}` | Retrieve all movements of a specific bank account or credit product |
| `GET` | `/api/v1/transactions/reports/product/{productId}` | Generate a general product report within `[startDate, endDate]` (`Java Streams`) |
| `GET` | `/api/v1/transactions/reports/card/{cardId}/last-movements` | Retrieve the **last 10 movements** of a card or product (`Java Streams`) |
| `POST` | `/api/v1/transactions/{id}/reverse` | Execute a compensating ledger reversal (marks original as `REVERSED`) |

## Inter-Service Communication & Observability
- **`pivot-ms-config` (`:8888`):** Fetches externalized rules (`TransactionRulesProperties`).
- **`pivot-ms-eureka` (`:8761`):** Registers service instance as `MS-TRANSACTION`.
- **`pivot-ms-customer` (`:8081`):** Verifies active customer status via `WebClient` + `Resilience4j` (2s timeout).
- **`pivot-ms-account` (`:8082`):** Updates account balances and retrieves applied commissions via `WebClient` + `Resilience4j` (2s timeout).
- **`pivot-ms-credit` (`:8083`):** Updates credit balances via `WebClient` + `Resilience4j` (2s timeout).
- **Observability:** Structured logs with `X-Correlation-Id` exported to **Promtail -> Loki -> Grafana (`:3000`)**.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7.0 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`).
- **Database Name:** `db_pivot_transaction`
- **Collection:** `transactions`

## How to Run (Local & Docker)
1. Ensure MongoDB (`:27017`), `ms-config` (`:8888`), `ms-eureka` (`:8761`), `ms-customer` (`:8081`), `ms-account` (`:8082`), and `ms-credit` (`:8083`) are running.
2. Run unit tests, Checkstyle, and generate the JaCoCo coverage report:
   ```powershell
   mvn clean test checkstyle:check jacoco:report
   ```
3. Start the microservice locally:
   ```powershell
   mvn spring-boot:run
   ```
4. Build and run as an independent Docker container:
   ```powershell
   docker build -t pivot-ms-transaction:2.0.0 .
   docker run -d -p 8084:8084 --name pivot-ms-transaction pivot-ms-transaction:2.0.0
   ```
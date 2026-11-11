# Pivot Banking System - Transaction Microservice (`pivot-ms-transaction`)

## Overview
Reactive microservice acting as the **Immutable Financial Ledger and Transaction Orchestrator** of the Pivot Banking System. It validates customer status against `ms-customer` (`:8081`), executes real-time balance updates on passive accounts in `ms-account` (`:8082`) and active credit products in `ms-credit` (`:8083`), records immutable audit trails (`previousBalance`, `newBalance`, `operationNumber`), and handles accounting reversals via compensating entries.

## Architecture, Design Patterns & Security
- **Architectural Style:** Hexagonal Architecture (Ports and Adapters) with strict separation of `domain`, `application`, and `infrastructure` layers.
- **Reactive Stack:** Spring Boot 4 + Spring WebFlux (Netty) + **RxJava 3** (`Single`, `Maybe`, `Flowable`, `Completable`).
- **SOLID & Design Patterns (Zero God Classes / Zero Spaghetti Code):**
  - **Single Responsibility Principle (SRP):** Dedicated Use Case implementations (`RegisterTransactionUseCaseImpl`, `ReverseTransactionUseCaseImpl`, `FindTransactionByIdUseCaseImpl`, `RetrieveTransactionUseCaseImpl`).
  - **Template Method Pattern (`AbstractTransactionStrategy`):** Encapsulates the invariant transaction lifecycle (active customer verification in `ms-customer`, remote balance execution, operation number generation, and ledger persistence) while delegating product-specific HTTP calls to concrete subclasses.
  - **Strategy Pattern (`TransactionExecutionStrategy`):** Four low-complexity concrete strategies (`DepositTransactionStrategy`, `WithdrawalTransactionStrategy`, `CreditPaymentTransactionStrategy`, `CreditConsumptionTransactionStrategy`).
  - **Factory / Registry Pattern (`TransactionStrategyRegistry`):** Provides $O(1)$ `EnumMap` lookup of transaction strategies at runtime without `switch` or `if-else` chains.
  - **Rich Enum & Compensating Action Pattern (`TransactionType`):** Self-contains product routing metadata and resolves its own inverse compensating operation (`DEPOSIT <-> WITHDRAWAL`, `CREDIT_PAYMENT <-> CREDIT_CONSUMPTION`) for transaction reversals.
  - **Immutable DTOs:** Java 17 `record` types across all web and client payloads.
- **Banking-Grade Security & Accounting Integrity:**
  - **Immutable Ledger Principle:** Financial transactions are never modified (`PUT`) or deleted (`DELETE`). Corrections are strictly performed via **Compensating Reversals (`POST /transactions/{id}/reverse`)**, which execute the inverse financial operation, mark the original entry as `REVERSED`, and persist a linked reversal record (`referenceTransactionId`).
  - **Masked Product Numbers:** Stores and returns only the masked account or card number (`************0-01` / `************5678`) received from downstream microservices.

## API Endpoints (`/api/v1/transactions`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/transactions` | Register and execute a financial transaction (`DEPOSIT`, `WITHDRAWAL`, `CREDIT_PAYMENT`, `CREDIT_CONSUMPTION`) |
| `GET` | `/api/v1/transactions` | Retrieve all financial transactions |
| `GET` | `/api/v1/transactions/{id}` | Retrieve transaction by unique MongoDB ID |
| `GET` | `/api/v1/transactions/operation/{operationNumber}` | Retrieve transaction by unique banking operation code (`OP-YYYYMMDD-XXXXXXXX`) |
| `GET` | `/api/v1/transactions/customer/{customerId}` | Retrieve all transactions executed by a specific customer |
| `GET` | `/api/v1/transactions/product/{targetProductId}` | Retrieve all movements of a specific Bank Account or Credit Product[cite: 2] |
| `POST` | `/api/v1/transactions/{id}/reverse` | Execute a compensating reversal on a `COMPLETED` transaction |

## Inter-Service Communication
- **`pivot-ms-config` (`:8888`):** Fetches externalized configuration, downstream service URLs, and transaction rules (`TransactionRulesProperties`).
- **`pivot-ms-customer` (`:8081`):** Consumed via non-blocking `WebClient` (`GET /api/v1/customers/{id}`) to verify customer existence and `ACTIVE` status prior to any transaction.
- **`pivot-ms-account` (`:8082`):** Consumed via non-blocking `WebClient` (`PATCH /api/v1/accounts/{id}/balance`) to process `DEPOSIT` and `WITHDRAWAL` operations.
- **`pivot-ms-credit` (`:8083`):** Consumed via non-blocking `WebClient` (`PATCH /api/v1/credits/{id}/balance`) to process `PAYMENT` and `CONSUMPTION` operations.

## Database (Database per Service Pattern)
- **Engine:** MongoDB 7 (Reactive Spring Data MongoDB using derived query methods, zero `@Query`).
- **Database Name:** `db_pivot_transaction`
- **Collection:** `transactions` (with unique index on `operationNumber`, and indexes on `customerId` and `targetProductId`).

## How to Run
1. Ensure the MongoDB container (`:27017`), `ms-config` (`:8888`), `ms-customer` (`:8081`), `ms-account` (`:8082`), and `ms-credit` (`:8083`) are running.
2. Start the microservice:
```powershell
   mvn clean spring-boot:run
```

3. The service will start on port **`8084`**.
4. **API Testing:** Import **`pivot-ms-transaction.postman_collection.json`** into Postman to run all end-to-end test scenarios.

---

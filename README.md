# Splitwise MVP - Architectural Decisions

This document outlines the key technical decisions and architectural patterns adopted in the implementation of the Splitwise MVP backend.

## 1. Financial Precision

Maintaining accuracy in financial calculations is paramount. Two key decisions were made:

- **Use of `BigDecimal`:** All monetary values are stored and calculated using `java.math.BigDecimal`. Floating-point types like `double` or `float` were strictly avoided to prevent precision loss (e.g., `0.1 + 0.2 != 0.3` in standard floating-point arithmetic).
- **Cent Distribution Algorithm:** When dividing expenses (e.g., $10.00 split among 3 people), the system does not lose cents. The `EqualSplitStrategy` calculates base shares rounded down and distributes the remainder cents to the first participants ensuring uniqueness and that `sum(shares) == total_amount`.
  - Example: $100 / 3 => $33.34, $33.33, $33.33.

## 2. Scalability: UserBalance Entity

A naive implementation for calculating "Who owes whom" would involve summing up all unsettled expense shares between two users each time the balance is requested. This would be an `O(N)` operation where N is the number of transactions history.

To ensure `O(1)` read performance for balances:
- **`UserBalance` Entity:** We introduced a consolidated balance entity that is updated atomically with every expense creation.
- **Unique Constraint:** A composite unique constraint ensures only one record exists per pair of users `(from_user_id, to_user_id)`.
- **ID Convention:** To avoid duplicate pairs like (A, B) and (B, A), we adhere to a convention where `from_user_id < to_user_id` is enforced at the service level (or DB constraint level in a real-world scenario). The sign of the `balance` field indicates the direction of debt:
  - Positive: `fromUser` owes `toUser`.
  - Negative: `toUser` owes `fromUser`.

This design allows for instant retrieval of balances for the dashboard without heavy aggregation queries.

## 3. Extensibility: Strategy Pattern

The application is designed to support multiple expense splitting methods (Equal, Exact Amount, Percentage, Shares) without modifying the core `ExpenseService`.

- **`SplitStrategy` Interface:** Defines the contract for splitting logic.
- **Implementations:**
  - `EqualSplitStrategy`: Handles equal division logic.
  - `ExactAmountStrategy`: Validates and delegates exact inputs.
- **Factory Pattern:** `SplitStrategyFactory` selects the appropriate strategy based on the `SplitType` enum at runtime.

This adheres to the Open/Closed Principle (OCP), allowing new splitting algorithms to be added as new classes.

## 4. Layered Architecture

We followed a strict layered architecture:
- **Controllers:** Pure delegates. They handle HTTP concerns (mapping, status codes) and delegate business logic immediately to Services.
- **Services:** encapsulate all business rules (e.g., creating users, calculating splits, updating balances transactionally).
- **DTOs vs Entities:** Validation annotations (`@NotNull`, `@Email`, etc.) are placed on DTOs to sanitise input at the API boundary, keeping JPA Entities focused on data persistence and schema definition.

## 5. Logging

SLF4J is used for logging critical business logic steps, such as the distribution of remainder cents in the splitting strategy, aiding in debugging and auditability.

# Integration Test Plan

## Goal
Verify that controllers, services, repositories, validation, and database persistence work together for the MVP API.

## Scope
- REST endpoints defined in `project_docs/07-documentation/API-contract.md`.
- JDBC/MySQL persistence behavior (`INSERT`, `SELECT`, `UPDATE`, `DELETE`).
- Dashboard summary aggregation endpoint.
- External stock API integration behavior (success and fallback/error paths).

## Test Environment
- Spring Boot test profile: `application-test.properties`.
- Database:
  - Preferred: Testcontainers MySQL 8 for SQL fidelity.
  - Acceptable fallback: H2 in MySQL compatibility mode for faster local iteration.
- HTTP tests: `MockMvc` or `WebTestClient`.
- External API calls mocked using a stub (WireMock or mock service bean).

## Core Integration Scenarios

### CRUD Flows
1. Create stock holding -> retrieve by ID -> appears in list.
2. Update quantity/current price -> summary totals update correctly.
3. Delete holding -> item absent from list and summary counts decrement.
4. Filter list by `type` returns only that asset class.

### Dashboard Flows
1. Mixed portfolio (stocks, bonds, mutual funds) returns correct totals.
2. Allocation percentages by type are correct and stable with rounding.
3. Empty portfolio returns all zeros.

### External Stock Data Flows
1. Stock create without `currentPrice` fetches quote from sample market API wrapper.
2. Quote refresh endpoint updates stored `currentPrice` when API succeeds.
3. Quote refresh endpoint returns `502` when upstream API fails.
4. Create/update still succeeds with stale/manual price when upstream is down.

### Validation/Error Flows
1. Invalid payload returns `400` with `fieldErrors` array.
2. Missing item ID returns `404`.
3. Unsupported enum value returns `400`.

## Entry Criteria
- Unit tests for core services/controllers pass.
- Schema script (`schema.sql`) is available and aligned with model.
- API contract and DTO fields are synchronized.

## Exit Criteria
- All high-priority scenarios pass.
- No data corruption across create/update/delete flows.
- No unhandled exception stack traces in integration test logs.

## Suggested Test Data Set
- 3 stocks (e.g., AAPL, TSLA, AMZN)
- 2 bonds
- 2 mutual funds
- Include one low-volume and one high-volume position for calculation confidence.

## Commands
```powershell
.\mvnw verify
.\mvnw -Dtest=*IntegrationTest test
```


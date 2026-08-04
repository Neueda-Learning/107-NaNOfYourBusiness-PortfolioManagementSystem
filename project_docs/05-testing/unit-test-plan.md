# Unit Test Plan

## Goal
Validate business logic quickly and repeatedly for a single-user Portfolio Management API before integration testing.

## In Scope
- Service-layer logic for portfolio CRUD and dashboard calculations.
- Controller behavior via `@WebMvcTest` with mocked services.
- Mapper and utility logic that transforms model-to-response values.
- External stock quote fallback behavior in `MarketDataService` wrappers.

## Out of Scope
- Real database wiring (covered in integration tests).
- Full end-to-end UI behavior (covered in frontend/manual testing).
- Load/performance behavior (covered in `performance-tests.md`).

## Test Stack
- JUnit 5
- Mockito
- Spring Boot Test (`@WebMvcTest` for controller slices)
- AssertJ (recommended for readable assertions)

## Coverage Targets (MVP)
- Line coverage: >= 75% for service package.
- Branch coverage: >= 65% for validation-heavy service methods.
- Controller endpoint coverage: all MVP endpoints in `API-contract.md`.

## Unit Test Areas

### 1) Portfolio Item Service
- Create item with valid payload (`STOCK`, `BOND`, `MUTUAL_FUND`) returns persisted response.
- Update existing item recalculates derived fields (`currentValue`, `gainLoss`, `gainLossPercent`).
- Delete existing item succeeds; delete missing item throws `ResourceNotFoundException`.
- Filter by type returns only requested asset type.
- For stock items, missing `currentPrice` triggers market data fetch attempt.
- If market data fetch fails, service keeps user-provided or last-known price (graceful fallback).

### 2) Portfolio Summary Service
- Aggregates total value, total cost, and overall gain/loss correctly.
- Allocation percentages by type sum to ~100% (allow rounding tolerance).
- Empty portfolio returns zeros and empty allocation list.

### 3) Controllers (`@WebMvcTest`)
- Status code checks: 200/201/204 happy paths.
- Validation errors return 400 with standard error body shape.
- Not found errors return 404.
- External API failure mapping returns 502 for refresh endpoint (if implemented).

### 4) Validation
- Reject negative/zero quantity and purchase price.
- Reject missing `type`, `symbolOrName`, `purchaseDate`.
- Reject future `purchaseDate`.
- Reject unsupported enum values.

## Data Design for Tests
Use deterministic fixtures with simple values for easy math checks:
- `STOCK`: AAPL, qty 10, purchase 100, current 120.
- `BOND`: GOVT10Y, qty 5, purchase 95, current 98.
- `MUTUAL_FUND`: INDEX500, qty 20, purchase 50, current 54.

## Definition of Done
- All unit tests pass in CI.
- No flaky tests across three consecutive local runs.
- Failed test messages clearly explain the business rule that broke.

## Commands
```powershell
.\mvnw test
.\mvnw -Dtest=*ServiceTest test
.\mvnw -Dtest=*ControllerTest test
```


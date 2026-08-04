# Functional Requirements - Portfolio Manager

## Scope

These requirements define MVP behavior for backend API and frontend UI. API request/response shapes must align with `project_docs/07-documentation/API-contract.md`.

## Portfolio Item Management

### FR-01: Create Portfolio Item
The system shall allow creating a portfolio item with at least:
- `type` (STOCK, MUTUAL_FUND, BOND)
- `symbolOrName`
- `quantity`
- `purchasePrice`
- `purchaseDate`
- optional `currentPrice`

### FR-02: List Portfolio Items
The system shall return all portfolio items and support filtering by asset type (e.g., `type=STOCK`).

### FR-03: Get Portfolio Item by ID
The system shall return a single portfolio item by unique identifier.

### FR-04: Update Portfolio Item
The system shall allow updating an existing portfolio item by identifier.

### FR-05: Delete Portfolio Item
The system shall allow deleting an existing portfolio item by identifier.

## Dashboard and Analytics

### FR-06: Portfolio Summary
The system shall provide a dashboard summary endpoint including:
- total portfolio value
- total gain/loss
- total item count
- allocation breakdown by asset type

### FR-07: Graphical Allocation View
The frontend shall display allocation data as a chart (doughnut/pie).

### FR-08 (Stretch): Performance Over Time
The system may provide time-series performance data and the frontend may display a line chart.

## Frontend User Flows

### FR-09: Tabbed Navigation
The frontend shall provide tabs for Dashboard, Stocks, Mutual Funds, and Bonds.

### FR-10: Asset-Type Tables
The frontend shall display each asset type in a tabular/list view with key values:
- symbol/name
- quantity
- purchase price
- current price
- gain/loss

### FR-11: Add Item Flow
The frontend shall provide a form to add an item and refresh the relevant tab and dashboard after success.

### FR-12: Remove Item Flow
The frontend shall provide a remove action with confirmation and update UI state on success.

### FR-13: Loading and Error States
The frontend shall show loading feedback during API calls and readable error messages on failures.

## External Market Data

### FR-14: Stock Price Enrichment
The backend shall integrate with an external market data service for stock price enrichment.

### FR-15: Fallback Behavior
If external price retrieval fails or times out, the system shall return last-known stored price data rather than failing all portfolio reads.

### FR-16: Controlled Price Refresh
The system shall avoid forcing external API calls on every list read and use controlled refresh behavior (e.g., explicit refresh endpoint, scheduled refresh, or cached retrieval).

## Validation and Error Handling

### FR-17: Input Validation
The system shall validate required fields, positive numeric values, and purchase date constraints before persistence.

### FR-18: Standard Error Responses
The system shall return consistent error responses for validation errors, not found resources, external API failures, and unexpected errors.

## Documentation and Testability

### FR-19: API Discoverability
The system shall provide OpenAPI/Swagger documentation for implemented endpoints.

### FR-20: Test Coverage for Critical Flows
The project shall include tests for CRUD happy paths, validation failures, not-found cases, and external API fallback behavior.

## Stretch Requirements (Optional)

### FR-21: Edit Flow in Frontend
The frontend may provide an edit modal/flow for existing items.

### FR-22: AI/Quantum Experimental Features
The project may include clearly marked experimental endpoints or demos for AI/Quantum exploration after MVP completion.

## References

- `project_docs/07-documentation/about.md`
- `project_docs/07-documentation/Backend-plan.md`
- `project_docs/07-documentation/Frontend-plan.md`


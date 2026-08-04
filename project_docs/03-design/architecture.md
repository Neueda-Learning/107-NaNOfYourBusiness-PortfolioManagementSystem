# Architecture Design - Portfolio Management System

This document describes the target architecture for the Portfolio Management REST API.
It focuses on MVP delivery first, then phased enhancements.

## 1. Architecture Goals

- Deliver a reliable REST API for managing portfolio items.
- Keep design simple for a training project (single service, single database).
- Support portfolio item CRUD and summary reporting.
- Allow external stock price enrichment without making core flows brittle.

## 2. System Context

- Primary actor: single portfolio user.
- Frontend client: web UI consuming REST endpoints.
- Backend: Spring Boot API.
- Data store: relational database (MySQL training stack).
- External integration: cached market price API for stock quote refresh.

## 3. High-Level Component View

```text
[Web Frontend]
      |
      v
[Portfolio REST API - Spring Boot]
  |        |         |
  v        v         v
Controller Service Repository
                  |
                  v
             [MySQL DB]

Service ---> [External Market Data API] (for stock refresh)
```

## 4. Backend Layer Responsibilities

### 4.1 Controller Layer

- Expose `/api/v1` endpoints.
- Validate request payloads and query params.
- Return consistent HTTP status codes and response DTOs.
- Avoid embedding business rules in controller methods.

### 4.2 Service Layer

- Own business logic for create/update/delete/list/read operations.
- Compute derived fields (`currentValue`, `gainLoss`, `gainLossPercent`).
- Coordinate external stock price refresh with graceful fallback.
- Provide summary aggregation values for dashboard cards and charts.

### 4.3 Repository Layer

- Use `JdbcTemplate` and explicit SQL queries.
- Map relational rows to domain models.
- Handle filtering (`type`) and common aggregate queries.
- Keep SQL aligned with `database-schema.md`.

## 5. Domain Model

### Core Entity: PortfolioItem

Minimal MVP fields:

- `id`
- `type`
- `symbolOrName`
- `quantity`
- `purchasePrice`
- `purchaseDate`
- `currentPrice`
- `createdAt`
- `updatedAt`

Optional phase 2 fields are documented in `database-schema.md`.

## 6. Integration Design (External Price Source)

- External calls should happen through a dedicated service (`MarketDataService`).
- External API errors must not break core CRUD.
- Normal reads return stored last-known `currentPrice`.
- Explicit refresh endpoint (`POST /portfolio-items/{id}/refresh-price`) updates price on demand.

## 7. Non-Functional Design Decisions

### Reliability

- Degrade gracefully when external market service fails.
- Use reasonable HTTP timeout and exception handling around external calls.

### Maintainability

- Keep clear separation between controller/service/repository.
- Keep API contract and schema documents in sync with code changes.

### Performance

- Avoid calling external pricing API on every list/read endpoint call.
- Consider caching quote lookups or periodic refresh jobs in later iterations.

### Security (MVP)

- No authentication required in v1 by project requirement.
- Validate inputs and return safe error messages.

## 8. Deployment View (Local Training Setup)

```text
Developer Machine
  |- Spring Boot app (localhost:8080)
  |- MySQL instance
  |- Frontend static site/dev server
```

CORS should permit local frontend origins for development.

## 9. Architecture Evolution Path

### MVP (Phase 1)

- CRUD endpoints for portfolio items.
- Summary endpoint for dashboard totals and allocation.
- Manual + optional refreshed stock current price.

### Phase 2

- Type-specific fields for stocks/bonds/funds.
- Better caching/refresh strategy.
- Performance-over-time endpoint.

### Stretch

- AI insights endpoint(s) and experimental quantum optimization pathway.

## 10. Traceability

- API details: `project_docs/03-design/api-contracts.md`
- Data model and SQL: `project_docs/03-design/database-schema.md`
- Flows: `project_docs/03-design/sequence-diagrams.md`
- Implementation planning: `project_docs/04-development/sprint-plan.md`


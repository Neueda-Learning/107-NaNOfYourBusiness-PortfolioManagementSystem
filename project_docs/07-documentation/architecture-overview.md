# Architecture Overview

## System Context
Portfolio Manager is a single-user financial tracking system with a Spring Boot REST backend and a lightweight frontend dashboard.

## Functional Scope (MVP)
- Full CRUD for holdings.
- Asset classes: `STOCK`, `BOND`, `MUTUAL_FUND`.
- Dashboard with total value, profit/loss, and allocation graph.
- Separate UI tabs for Stocks, Bonds, Mutual Funds.
- Stock price refresh using an external sample market API.
- Customer support access via help tab/contact section.

## Planned Extensions (Phase 2)
- SIP-focused workflows (recurring investment tracking).
- Real estate holdings.
- AI insights (recommendations, natural language summary/query).
- Quantum proof-of-concept optimization experiments.

## High-Level Components
1. **Frontend UI**
   - Renders dashboard and per-asset tabs.
   - Calls REST endpoints under `/api/v1`.
2. **REST Controllers**
   - Portfolio CRUD endpoints.
   - Dashboard summary endpoints.
3. **Service Layer**
   - Business rules, calculations, and stock quote fallback policy.
4. **Repository Layer**
   - SQL persistence using JDBC template.
5. **MySQL Database**
   - Stores portfolio holdings and price snapshots.
6. **External Market API**
   - Quote source for stocks (resilient and optional per request flow).

## Data Flow
1. User creates/updates a stock holding.
2. Service optionally fetches latest quote.
3. Repository stores normalized data in `portfolio_item` table.
4. Dashboard summary endpoint aggregates totals and allocation.
5. Frontend updates cards and charts.

## Design Principles
- Start small, then evolve.
- Keep API contract stable and explicit (`API-contract.md`).
- Fail gracefully on upstream market data outages.
- Prefer readable, testable layered code over framework complexity.


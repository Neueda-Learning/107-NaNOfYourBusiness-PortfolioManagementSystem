# API Guide

This guide explains how to use the Portfolio Manager REST API in day-to-day development and testing.

## Base Path
- `/api/v1`

See authoritative endpoint/field definitions in `project_docs/07-documentation/API-contract.md`.

## Common Workflows

### 1) Add a Stock Holding
```http
POST /api/v1/portfolio-items
Content-Type: application/json

{
  "type": "STOCK",
  "symbolOrName": "TSLA",
  "quantity": 15,
  "purchasePrice": 180.50,
  "purchaseDate": "2026-01-10"
}
```
- If `currentPrice` is omitted, backend may fetch it from market API.

### 2) List Holdings
- All holdings: `GET /api/v1/portfolio-items`
- Stocks only: `GET /api/v1/portfolio-items?type=STOCK`
- Bonds only: `GET /api/v1/portfolio-items?type=BOND`
- Mutual funds only: `GET /api/v1/portfolio-items?type=MUTUAL_FUND`

### 3) Update and Delete
- Update: `PUT /api/v1/portfolio-items/{id}`
- Delete: `DELETE /api/v1/portfolio-items/{id}`

### 4) Dashboard Data
- Summary cards + allocation chart: `GET /api/v1/portfolio/summary`
- Performance line chart (stretch): `GET /api/v1/portfolio/performance?range=ALL`

### 5) Refresh Stock Quote
- `POST /api/v1/portfolio-items/{id}/refresh-price`
- Returns `502` if upstream market API is unavailable.

## Expected Error Pattern
All non-2xx responses follow the standard error shape documented in `API-contract.md`.

## Notes for Frontend Integration
- Treat money and quantity as numbers.
- Render computed fields (`currentValue`, `gainLoss`, `gainLossPercent`) as read-only.
- Show graceful warning when refresh returns `502` (keep last-known price).

## Customer Requirement Mapping
- Single user: no auth headers required.
- Portfolio tabs: driven by `type` filter.
- Profit/loss dashboard: provided by summary endpoint.
- Support/help experience: frontend-level section for contact/escalation details.
- SIP and Real Estate: document as Phase 2 extension unless API enum is expanded.


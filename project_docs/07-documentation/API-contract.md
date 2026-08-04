# API Contract — Portfolio Manager

> **This file is the single source of truth for the REST API.**
> Backend and frontend must both be built against exactly what's written here.
> If a field or endpoint needs to change, update this file first, then update the code on both sides.
> Give this file to Copilot alongside `Backend-plan.md` or `Frontend-plan.md` for any API-related work.

## 1. Conventions

- **Base URL**: `/api/v1`
- **Format**: JSON request/response bodies, `Content-Type: application/json`
- **Dates**: ISO-8601 (`YYYY-MM-DD` for dates, `YYYY-MM-DDTHH:mm:ss` for timestamps)
- **Numbers**: quantities/prices/money are JSON numbers with decimal precision (backend uses `BigDecimal`; frontend should treat them as numbers, not strings)
- **IDs**: numeric (`Long` on the backend)
- **Enum values** (`AssetType`): `"STOCK"`, `"BOND"`, `"MUTUAL_FUND"` — always uppercase, exactly as written here
- No authentication headers required (single-user app)

### Standard HTTP status codes

| Code | Meaning |
|---|---|
| 200 | Success (GET, PUT, or DELETE) |
| 201 | Created (POST) |
| 400 | Validation error |
| 404 | Resource not found |
| 502 | External stock price API unavailable (see §6) |
| 500 | Unexpected server error |

### Standard error response shape

Every non-2xx response returns this shape:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "quantity must be greater than 0",
  "timestamp": "2026-08-03T10:15:30",
  "fieldErrors": [
    { "field": "quantity", "message": "must be greater than 0" }
  ]
}
```

`fieldErrors` is only present for `400` validation failures; omit it otherwise.

## 2. Data Model — `PortfolioItem`

Fields marked **(Phase 2)** are optional/type-specific and can be added after the MVP CRUD flow works — see `Backend-plan.md` §5.

| Field | Type | Applies to | Notes |
|---|---|---|---|
| `id` | number | all | server-generated, absent on create requests |
| `type` | string enum | all | `"STOCK"` \| `"BOND"` \| `"MUTUAL_FUND"`, required |
| `symbolOrName` | string | all | ticker for stocks, name/ISIN for bonds & funds, required |
| `quantity` | number | all | units/shares held, required, must be > 0 |
| `purchasePrice` | number | all | price per unit at purchase, required, must be > 0 |
| `purchaseDate` | string (date) | all | required, must not be in the future |
| `currentPrice` | number | all | last known price per unit; for stocks, refreshed from the external market data service |
| `currentValue` | number | all, **computed, response-only** | `quantity * currentPrice`, never sent on create/update |
| `gainLoss` | number | all, **computed, response-only** | `currentValue - (quantity * purchasePrice)` |
| `gainLossPercent` | number | all, **computed, response-only** | `gainLoss / (quantity * purchasePrice) * 100` |
| `createdAt` / `updatedAt` | string (timestamp) | all, **response-only** | |
| `couponRate`, `maturityDate`, `faceValue`, `issuer` | — | bonds **(Phase 2)** | |
| `expenseRatio`, `fundManager`, `category` | — | mutual funds **(Phase 2)** | |
| `sector`, `exchange` | — | stocks **(Phase 2)** | |

### Example: `PortfolioItemResponse`

```json
{
  "id": 12,
  "type": "STOCK",
  "symbolOrName": "AAPL",
  "quantity": 10,
  "purchasePrice": 150.25,
  "purchaseDate": "2025-01-15",
  "currentPrice": 195.40,
  "currentValue": 1954.00,
  "gainLoss": 451.50,
  "gainLossPercent": 30.05,
  "createdAt": "2025-01-15T09:00:00",
  "updatedAt": "2026-08-01T07:30:00"
}
```

### Example: `PortfolioItemRequest` (create/update body)

```json
{
  "type": "STOCK",
  "symbolOrName": "AAPL",
  "quantity": 10,
  "purchasePrice": 150.25,
  "purchaseDate": "2025-01-15",
  "currentPrice": 195.40
}
```

`currentPrice` is optional on create for stocks — if omitted, the backend should attempt to fetch it from the external market data service on save (falling back gracefully per `Backend-plan.md` §7 if that fails).

## 3. Endpoints — Portfolio Items

### `GET /api/v1/portfolio-items`

List items, optionally filtered by type.

- Query param: `type` (optional) — one of `STOCK`, `BOND`, `MUTUAL_FUND`; omit for all items
- **200** → array of `PortfolioItemResponse`

```
GET /api/v1/portfolio-items?type=STOCK
```

### `GET /api/v1/portfolio-items/{id}`

- **200** → single `PortfolioItemResponse`
- **404** → not found

### `POST /api/v1/portfolio-items`

- Body: `PortfolioItemRequest`
- **201** → created `PortfolioItemResponse`, with `Location` header set to `/api/v1/portfolio-items/{id}`
- **400** → validation error

### `PUT /api/v1/portfolio-items/{id}`

- Body: `PortfolioItemRequest`
- **200** → updated `PortfolioItemResponse`
- **404** → not found
- **400** → validation error

### `DELETE /api/v1/portfolio-items/{id}`

- **200** or **204** (pick one and keep it consistent — recommend **204 No Content**)
- **404** → not found

### `POST /api/v1/portfolio-items/{id}/refresh-price` *(stock price refresh, optional but recommended)*

Forces a refresh of `currentPrice` from the external market data service for a single stock item.

- **200** → updated `PortfolioItemResponse`
- **404** → not found
- **502** → external API unavailable (item's stale `currentPrice` is left unchanged)

## 4. Endpoints — Dashboard / Summary

### `GET /api/v1/portfolio/summary`

Aggregate figures for the dashboard cards + allocation chart.

- **200**:

```json
{
  "totalValue": 45230.75,
  "totalCost": 41000.00,
  "totalGainLoss": 4230.75,
  "totalGainLossPercent": 10.32,
  "itemCount": 14,
  "allocationByType": [
    { "type": "STOCK", "value": 30250.00, "percent": 66.9 },
    { "type": "BOND", "value": 8000.00, "percent": 17.7 },
    { "type": "MUTUAL_FUND", "value": 6980.75, "percent": 15.4 }
  ]
}
```

### `GET /api/v1/portfolio/performance` *(stretch goal)*

Time series for a performance-over-time line chart.

- Query param: `range` (optional) — e.g. `1M`, `3M`, `1Y`, `ALL`; default `ALL`
- **200**:

```json
[
  { "date": "2026-07-01", "totalValue": 43000.00 },
  { "date": "2026-07-15", "totalValue": 44100.50 },
  { "date": "2026-08-01", "totalValue": 45230.75 }
]
```

If the backend can't yet compute true historical performance (this needs stored historical snapshots, not just current state), leave this endpoint out of scope for the MVP and note it explicitly rather than faking data — the frontend's dashboard chart should degrade gracefully (hide the chart or show "not enough data yet") if this endpoint isn't implemented.

## 5. Endpoint Summary Table

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/portfolio-items?type=` | List items (optionally filtered) |
| GET | `/api/v1/portfolio-items/{id}` | Get one item |
| POST | `/api/v1/portfolio-items` | Create item |
| PUT | `/api/v1/portfolio-items/{id}` | Update item |
| DELETE | `/api/v1/portfolio-items/{id}` | Remove item |
| POST | `/api/v1/portfolio-items/{id}/refresh-price` | Refresh a stock's current price |
| GET | `/api/v1/portfolio/summary` | Dashboard totals + allocation |
| GET | `/api/v1/portfolio/performance?range=` | Time series for performance chart (stretch) |

## 6. External Stock Price Failures

When the external market data service is unavailable:

- `POST/PUT` on a stock item still succeeds — it just keeps the last known `currentPrice` (or the value the user supplied) rather than failing the whole request.
- `refresh-price` returns `502` with the standard error shape, and the frontend should show a small inline "price refresh failed, showing last known price" message rather than blocking the UI.

## 7. Change Log

Record any change to this contract here so both sides know when to re-sync:

| Date | Change |
|---|---|
| 2026-08-03 | Initial contract drafted |

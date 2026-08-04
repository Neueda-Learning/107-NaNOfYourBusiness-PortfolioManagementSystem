# Functional Test Cases

## Scope
MVP single-user portfolio features: manage holdings, view dashboard profit/loss, filter by asset tabs, and handle stock quote refresh.

## Legend
- Priority: `P0` critical, `P1` high, `P2` medium
- Type: API/UI/Manual

| ID | Requirement | Priority | Type | Preconditions | Steps | Expected Result |
|---|---|---|---|---|---|---|
| TC-01 | Add stock holding | P0 | API | Service running | `POST /portfolio-items` with valid stock payload | `201` created, response includes computed fields |
| TC-02 | Add bond holding | P0 | API | Service running | `POST /portfolio-items` with valid bond payload | `201` created |
| TC-03 | Add mutual fund holding | P0 | API | Service running | `POST /portfolio-items` with valid fund payload | `201` created |
| TC-04 | List all holdings | P0 | API/UI | Data exists | `GET /portfolio-items` | `200`, includes all asset types |
| TC-05 | Filter by stocks tab | P0 | API/UI | Mixed asset data exists | `GET /portfolio-items?type=STOCK` | Only stocks returned/rendered |
| TC-06 | Filter by bonds tab | P0 | API/UI | Mixed asset data exists | `GET /portfolio-items?type=BOND` | Only bonds returned/rendered |
| TC-07 | Filter by mutual funds tab | P0 | API/UI | Mixed asset data exists | `GET /portfolio-items?type=MUTUAL_FUND` | Only mutual funds returned/rendered |
| TC-08 | Update holding | P0 | API | Existing item | `PUT /portfolio-items/{id}` with changed quantity | `200`, recalculated gain/loss values |
| TC-09 | Delete holding | P0 | API/UI | Existing item | `DELETE /portfolio-items/{id}` | `204` (or agreed success code), removed from lists |
| TC-10 | Dashboard summary cards | P0 | API/UI | Portfolio has items | `GET /portfolio/summary` | Accurate total value, total cost, gain/loss, count |
| TC-11 | Dashboard allocation graph data | P1 | API/UI | Portfolio has mixed items | Read `allocationByType` from summary | Chart percentages and values are consistent |
| TC-12 | Validation: negative quantity | P0 | API | Service running | POST with `quantity <= 0` | `400` with field error for quantity |
| TC-13 | Validation: future purchase date | P0 | API | Service running | POST with future `purchaseDate` | `400` with field error |
| TC-14 | Not found by ID | P0 | API | ID does not exist | `GET /portfolio-items/{id}` | `404` error shape returned |
| TC-15 | Refresh stock price success | P1 | API | Existing stock with symbol | `POST /portfolio-items/{id}/refresh-price` | `200`, `currentPrice` updated |
| TC-16 | Refresh stock price upstream failure | P1 | API | External API unavailable | Call refresh endpoint | `502`, last-known price unchanged |
| TC-17 | Customer support path available | P2 | UI/Manual | Frontend running | Open support/help tab or link | User sees support contact guidance |
| TC-18 | Search stock list (if implemented) | P2 | API/UI | Market/search endpoint enabled | Query by ticker prefix | Matching tickers returned |

## MVP vs Phase 2 Notes
- SIP and Real Estate are customer-requested extensions; treat as Phase 2 unless `AssetType` is expanded.
- For MVP, SIP data can be represented as `MUTUAL_FUND` holdings plus notes in UI/documentation.


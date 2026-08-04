# Task Breakdown - Portfolio Management System

This backlog is ordered by implementation priority.
Focus on MVP completion before starting enhancement or stretch tasks.

## 1. MVP Task List (Priority Order)

| ID | Task | Output | Dependency |
|---|---|---|---|
| T1 | Finalize API contract | agreed `api-contracts.md` | none |
| T2 | Finalize DB schema | agreed `database-schema.md` + SQL draft | T1 |
| T3 | Create model + DTO classes | `AssetType`, `PortfolioItem`, request/response DTOs | T1, T2 |
| T4 | Implement repository layer | CRUD SQL + row mapper | T3 |
| T5 | Implement service layer | CRUD business logic + computed fields | T4 |
| T6 | Implement controllers | REST endpoints under `/api/v1` | T5 |
| T7 | Add validation + exception handler | standardized 400/404/500 responses | T6 |
| T8 | Add controller/service tests | passing unit and WebMvc tests | T7 |
| T9 | Implement summary endpoint | totals + allocation response | T5 |
| T10 | Test end-to-end flow | manual API verification script/checklist | T8, T9 |

## 2. Enhancement Tasks (After MVP)

| ID | Task | Output | Dependency |
|---|---|---|---|
| E1 | Integrate market data service | quote fetch client/service | T6 |
| E2 | Implement refresh-price endpoint | `/portfolio-items/{id}/refresh-price` | E1 |
| E3 | Add external failure fallback behavior | 502 path + stale-price behavior | E2 |
| E4 | Add repository tests for aggregate queries | verified summary SQL | T9 |
| E5 | Improve logging and observability | structured logs and error tracing | T7 |

## 3. Stretch Tasks (Optional)

| ID | Task | Output | Dependency |
|---|---|---|---|
| S1 | Portfolio performance endpoint design | `/portfolio/performance` contract draft | T9 |
| S2 | AI insight PoC design | `/portfolio/predictions` proposal | T10 |
| S3 | Quantum optimization research brief | documented findings + references | none |
| S4 | AI/Quantum demo preparation | presentation artifacts | S2 or S3 |

## 4. Workstream Grouping

- Workstream A (Backend Core): T3, T4, T5, T6
- Workstream B (Quality): T7, T8, T10
- Workstream C (Analytics): T9, E4
- Workstream D (Integration): E1, E2, E3
- Workstream E (Exploration): S1-S4

## 5. Suggested Ownership Template

| Task ID | Owner | Reviewer | Status |
|---|---|---|---|
| T1 | TBD | TBD | TODO |
| T2 | TBD | TBD | TODO |
| T3 | TBD | TBD | TODO |
| T4 | TBD | TBD | TODO |
| T5 | TBD | TBD | TODO |
| T6 | TBD | TBD | TODO |
| T7 | TBD | TBD | TODO |
| T8 | TBD | TBD | TODO |
| T9 | TBD | TBD | TODO |
| T10 | TBD | TBD | TODO |

## 6. Estimation Bands

Use simple effort bands for planning:

- `S` (0.5 day): focused change, low complexity
- `M` (1 day): moderate implementation and testing
- `L` (2+ days): multi-layer task or integration-heavy

Suggested initial sizing:

- T1 `S`, T2 `S`, T3 `M`, T4 `L`, T5 `L`, T6 `M`, T7 `M`, T8 `M`, T9 `M`, T10 `S`

## 7. Completion Checklist per Task

- Acceptance criteria clearly written.
- Code implementation complete.
- Tests added/updated and passing.
- Relevant docs updated.
- PR reviewed and merged.


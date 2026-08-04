# Sprint Plan - Portfolio Management System

This sprint plan is designed for a training project with MVP-first delivery.
It can be run as a 2-week plan or compressed into one intensive week.

## 1. Sprint Objectives

### Objective A: Deliver MVP REST API

- Portfolio item CRUD endpoints.
- Validation and standardized error responses.
- Database-backed persistence for all core fields.

### Objective B: Enable Dashboard Data

- Portfolio summary endpoint with totals and allocation by type.
- Computed gain/loss metrics in item and summary responses.

### Objective C: Integrate External Price Refresh

- On-demand refresh endpoint for stock prices.
- Graceful degradation when external API fails.

## 2. Suggested Timeline

## Sprint 1 (Core MVP)

- Day 1: finalize design docs and schema.
- Day 2-3: implement model/repository/service/controller for CRUD.
- Day 4: add validation, global exception handling, and tests.
- Day 5: stabilize API contract compliance and basic frontend integration.

## Sprint 2 (Stabilization + Enhancements)

- Day 1-2: portfolio summary endpoint and aggregate calculations.
- Day 3: external stock refresh endpoint + fallback handling.
- Day 4: test hardening, bug fixes, and docs sync.
- Day 5: demo prep and stretch-goal spike (AI/Quantum research or PoC).

## 3. Backlog Themes

- API contract implementation
- persistence and SQL reliability
- input validation and safe error handling
- summary analytics for dashboard
- external integration resilience
- test coverage and regression safety

## 4. Dependencies

- Agreed API contract and schema must exist before coding starts.
- Local DB availability is required for repository development.
- External market API endpoint must be reachable for refresh feature.
- Frontend integration depends on stable endpoint paths and payload shapes.

## 5. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Overly complex data model early | delivery delay | enforce MVP fields only |
| API-contract drift | integration bugs | contract-first updates + review checklist |
| External API downtime | feature instability | fallback to last-known prices, return clear errors |
| Limited test coverage | regressions | enforce test cases in each story |
| Scope creep from stretch goals | MVP incomplete | defer AI/Quantum to scheduled spike only |

## 6. Definition of Done (Sprint Level)

A sprint objective is complete when:

- required endpoints are implemented and manually verified,
- core automated tests pass,
- API and schema docs are updated,
- known issues are tracked with clear follow-ups,
- demo scenario runs end-to-end.

## 7. Ceremonies and Team Rhythm

- Daily stand-up: 10-15 minutes.
- Mid-sprint checkpoint with instructor/customer.
- End-of-sprint demo + retrospective.
- Lightweight backlog refinement after retro.

## 8. Stretch Goal Slot (Optional)

If MVP is complete early, prioritize one exploration only:

1. AI insight PoC endpoint (`/portfolio/predictions`), or
2. Quantum optimization research note with toy example.

Keep stretch work isolated from core API stability.

## 9. Deliverables by End of Plan

- Completed docs under `project_docs/03-design` and `project_docs/04-development`.
- Working backend endpoints aligned to design contract.
- Basic test suite and reproducible demo flow.


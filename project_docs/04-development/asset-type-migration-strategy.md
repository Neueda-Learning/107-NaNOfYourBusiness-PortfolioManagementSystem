# Asset-Type Service Migration Strategy

## Goal

Reduce merge conflicts while keeping the existing API stable by splitting asset-type-specific business logic into dedicated service handlers.

## Scope

- Keep current endpoints unchanged in `controller/PortfolioItemController.java`.
- Keep current persistence model unchanged in `repository/PortfolioItemRepository.java`.
- Incrementally move type-specific logic out of `service/PortfolioItemService.java`.

## Traceability to MVP User Stories

- `US-03` Browse Stocks (MVP)
- `US-04` Browse Mutual Funds (MVP)
- `US-05` Browse Bonds (MVP)
- `US-06` Add Holding (MVP)
- `US-08` Update Holding Data (MVP API)
- `US-10` Graceful External Data Failure (MVP)
- `US-11` Consistent API Errors (MVP)

## Implementation Plan

### Phase 1 - Introduce Handler Architecture (this PR)

Checklist:
- [x] Add `PortfolioItemTypeHandler` strategy interface.
- [x] Add `PortfolioItemTypeHandlerRegistry` with startup guard for full `AssetType` coverage.
- [x] Add handlers:
  - [x] `StockPortfolioItemTypeHandler`
  - [x] `BondPortfolioItemTypeHandler`
  - [x] `MutualFundPortfolioItemTypeHandler`
- [x] Refactor `PortfolioItemService` to delegate create/refresh behavior to handlers.
- [x] Keep API behavior unchanged for existing endpoints.
- [x] Add/adjust tests for service delegation and registry safety.

User stories impacted:
- `US-06`: Create flow remains stable while stock-specific auto-price stays isolated.
- `US-10`: Refresh flow still uses `MarketDataService` fallback behavior.

### Phase 2 - Move Asset-Type Business Rules

Checklist:
- [ ] Move stock-only validations and enrichments into `StockPortfolioItemTypeHandler`.
- [ ] Implement bond-specific validations (coupon/yield rules when fields are added).
- [ ] Implement mutual-fund-specific validations (NAV/category rules when fields are added).
- [ ] Remove type conditionals from `PortfolioItemService`.
- [ ] Add service tests per handler for asset rules.

User stories impacted:
- `US-03`, `US-04`, `US-05`, `US-08`.

### Phase 3 - Browse-by-Type Optimization

Checklist:
- [ ] Add type-focused read services for browse tabs if logic diverges significantly.
- [ ] Keep shared controller contract (`type` filter) to avoid frontend churn.
- [ ] Add pagination/filter tuning per tab only if required by acceptance criteria.
- [ ] Add controller integration tests for per-type browse behavior and empty states.

User stories impacted:
- `US-03`, `US-04`, `US-05`, `US-11`.

### Phase 4 - Team Workflow Hardening (conflict reduction)

Checklist:
- [ ] Define file ownership by package (`service/portfolio/stock`, `service/portfolio/bond`, `service/portfolio/mutualfund`).
- [ ] Keep PRs single-concern and small (target under 300 LOC net change).
- [ ] Require daily rebase against main for active branches.
- [ ] Require test evidence in each PR.

User stories impacted:
- Indirectly supports all MVP stories by reducing delivery friction.

## Risk and Rollback

- Risk: behavior drift during extraction.
  - Mitigation: keep controller/repository unchanged and preserve existing tests.
- Risk: incomplete handler coverage.
  - Mitigation: registry startup guard fails fast if any `AssetType` is unhandled.
- Rollback: revert handler wiring in `PortfolioItemService` to previous direct logic in one PR.

## Definition of Done for Migration

- API contract unchanged for existing endpoints.
- Existing tests pass.
- New handler tests pass.
- No asset-type conditionals remain in orchestration logic where handlers exist.
- Developer ownership model documented and adopted in PR workflow.


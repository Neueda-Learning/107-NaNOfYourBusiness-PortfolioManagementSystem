# Dashboard Improvement Plan — Portfolio Manager

This plan targets the Dashboard tab only (`index.html` → `data-panel="dashboard"`,
`js/dashboard.js`, `js/charts.js`). It is split into **Visual** improvements and
**Feature** additions, each mapped back to user stories so scope stays traceable.

Current state (baseline, verified in code):

- ✅ US-01 Summary cards: Total Value, Total Gain/Loss (+%), Holdings count.
- ✅ US-02 Allocation doughnut chart by asset type (Chart.js, theme-aware).
- ⚠️ US-14 Loading/error/empty states exist for the dashboard as a whole, but
  individual widgets have no independent skeleton states.
- ❌ US-15 Performance-over-time chart is a static "Coming soon" placeholder —
  no backend endpoint (`/portfolio/performance`) exists yet.
- No trend/delta indicators, no top movers, no diversification insight, no
  date-range control, no export, no drill-down from chart back to holdings.

## Guiding Constraints

- Keep plain HTML/CSS/JS, no framework, no build step (per `Frontend-plan.md`).
- Keep `api.js` as the only fetch boundary; any new backend need must be added
  to `api-contracts.md` first, then implemented, then consumed.
- Every backend change must update: contract doc → implementation → tests →
  API docs (per `api-contracts.md` §7).
- Preserve existing design tokens in `variables.css` — reuse `--color-gain`,
  `--color-loss`, `--color-stock/bond/mutual-fund`, spacing/radius scale.

---

## Part A — Visual Improvements (no new backend needed)

### A1. Summary card polish
- [x] Add a small **sparkline** (mini inline SVG/Chart.js line, last N refreshes
  cached client-side) behind each summary card for a quick trend cue.
- [x] Add a subtle **icon** per card (value, gain/loss, holdings) consistent
  with the existing stroke-icon style used elsewhere in `index.html`.
- [x] Animate number count-up on load/refresh (reuse `animations.js` easing
  tokens: `--ease-out`, `--duration-base`).
- [x] Add a 4th card: **Total Cost Basis** (`totalCost` is already in the
  summary payload but unused in the UI) — cheap win, zero backend change.

### A2. Allocation chart polish
- [x] Add center label in the doughnut (total value) using a Chart.js plugin —
  currently the middle is empty space.
- [x] Add click-to-filter: clicking a legend segment (or wedge) jumps to that
  asset type's tab (Stocks/Bonds/Mutual Funds), reusing existing tab-switch
  logic in `app.js`.
- [ ] Add a compact **legend-as-table** fallback under the chart on narrow
  viewports (current legend text truncates on small screens). *(deferred —
  low priority, existing legend still wraps acceptably)*

### A3. Layout & responsiveness
- [x] Convert `.summary-cards` to an explicit responsive grid: 4 → 2 → 1
  columns across desktop/tablet/mobile breakpoints.
- [x] Add per-widget skeleton loaders (card-shaped shimmer) instead of one
  global spinner, so cards/chart appear as a cohesive placeholder while
  loading — closes the remaining gap in US-14.
- [x] Add a **"Last updated"** timestamp + manual refresh button in the
  dashboard toolbar, consistent with the pattern already used in the Stocks
  tab (`browse-prices-status`, `browse-refresh-holdings-btn`).

### A4. Empty/error state polish
- [x] Empty state: add a direct CTA button ("Add your first holding") that
  jumps to the Stocks tab, instead of just descriptive text.
- [x] Error state: differentiate "network error" vs "server error" messaging
  using the existing standardized error contract (`error` field).

---

## Part B — Feature Additions

### B1. Performance Over Time chart (completes US-15)
Backend work required — add to `api-contracts.md` first:

- [ ] New endpoint `GET /api/v1/portfolio/performance?range=1M|3M|6M|1Y|ALL`
  returning `{ range, points: [{ date, totalValue, totalCost }] }`.
- [ ] Backend approach: since there's no stored portfolio-value history table,
  compute on the fly by reusing existing per-instrument history endpoints
  (stock `/market/{ticker}/history`, fund `/api/mutual-funds/{code}/history`),
  weighting by each holding's quantity as of each date (quantity = 0 before
  purchase date; adjust for buy/sell trade records where available).
- [ ] Add a lightweight `PortfolioPerformanceService` that aggregates per-day
  values across all holdings and returns a merged time series.
- [ ] Cache aggregated results briefly (e.g. 5 min) to avoid recomputing on
  every dashboard load — mirrors the existing scheduled-poll cache pattern
  used for stock quotes.
- [ ] Frontend: replace the placeholder card with a Chart.js line chart
  (`renderPerformanceChart` in `charts.js`), range toggle buttons matching the
  existing NAV/price history modal pattern (`1M 3M 6M 1Y All`).
- [ ] Show cost-basis as a second (dashed) line for gain/loss visual context.

### B2. Top Movers / Best & Worst Performer widget
- [x] New small card: "Top Gainer" and "Top Loser" among current holdings,
  computed client-side from `getPortfolioItems()` (no backend change needed —
  gainLossPercent is already returned per item).
- [x] Clicking an entry jumps to that item's asset-type tab.

### B3. Diversification / Concentration Insight
- [x] Small insight text under the allocation chart, e.g. "62% of your
  portfolio is in Stocks — consider diversifying" once one asset type crosses
  a configurable threshold (e.g. >60%). Pure frontend logic using existing
  `allocationByType` percentages.

### B4. Recent Activity feed
- [ ] Requires backend: a lightweight `trade_history` / `activity_log` table
  (buy/sell/add/remove events with timestamp) — currently not persisted per
  the DB schema review. Add as a Phase 2 schema addition.
- [ ] Dashboard widget: last 5 actions ("Bought 10 AAPL on Aug 4", "Removed
  BOND X on Aug 1") with relative timestamps.
- [ ] This also directly supports auditability (a common non-functional
  requirement) — check `non-functional-requirements.md` before implementing.

### B5. Holdings quick-filter / drill-down
- [ ] Make summary cards and chart segments clickable to pre-filter the
  destination tab (ties into A2 click-to-filter).

### B6. Export / Share
- [ ] "Export summary as CSV/PDF" button — pure frontend using `summary` +
  `getPortfolioItems()` data already in memory (e.g. `jsPDF`/manual CSV blob,
  no backend change).

### B7. Currency/locale toggle (nice-to-have)
- [ ] Since Mutual Funds use ₹ and Stocks use $, consider a normalized display
  currency toggle on the dashboard, or clearly label mixed-currency totals if
  `totalValue` currently mixes them (**flag this as a correctness question for
  the backend summary calculation — verify with `PortfolioSummaryController`
  before assuming totals are apples-to-apples**).

### B8. Goal tracking (stretch)
- [ ] Let user set a target portfolio value; show progress bar on dashboard.
  Needs a simple `user_settings` or local-storage-only implementation (no
  backend needed if scoped to local storage for MVP+).

---

## Suggested Delivery Order

| Phase | Items | Backend needed? | Maps to |
|---|---|---|---|
| 1 | A1, A2, A3, A4 (visual polish) | No | US-01, US-02, US-14 |
| 2 | B2, B3, B5 (client-side smart widgets) | No | US-01, US-02 (extends) |
| 3 | B1 Performance chart | Yes — new endpoint + service | US-15 |
| 4 | B6, B7, B8 (export, currency, goals) | Partial | Post-MVP polish |
| 5 | B4 Recent Activity | Yes — new table + logging | Stretch / NFR audit |

## Verification Checklist Before Marking Done

- [ ] `api-contracts.md` updated for any new/changed endpoint (B1, B4).
- [ ] `database-schema.md` updated if `trade_history`/`activity_log` added.
- [ ] Unit/service tests added for `PortfolioPerformanceService`.
- [ ] Dashboard still degrades gracefully if market data is down (US-10 —
  performance chart should show stale/cached data, not break the whole tab).
- [ ] Dark/light theme verified for every new visual element (reuse CSS
  custom properties, do not hardcode colors — see `charts.js` pattern).
- [ ] Responsive check at laptop width minimum (per Frontend-plan §7).




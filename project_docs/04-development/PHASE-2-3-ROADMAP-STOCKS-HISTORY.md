# Phase 2 & 3 Roadmap — Enhanced Stocks Tab with History Chart

**Target Completion:** 2-3 business days (after Phase 1 verification)

---

## Phase 2: Resilience & Refresh Hardening (1 day)

**Goals:** Ensure controlled refresh behavior and stale-price messaging

### STK-06 & STK-09: Explicit Refresh Behavior Control
- **Requirement:** Stock price reads should never force live API fetch (cache-first)
- **Implementation:**
  - Verify `MarketDataService` cache is populated on 10s schedule
  - Confirm `GET /api/v1/portfolio-items` returns cached prices (no blocking fetch)
  - Dedicated endpoint `POST /api/v1/portfolio-items/{id}/refresh-price` for explicit refresh
  - Show "Source: live cache (~10s)" tooltip on normal reads
  - Show "Source: refreshed from market" after explicit refresh
- **Testing:**
  - Load Stocks tab, kill market API server
  - Verify holdings still load with cached prices
  - Verify UI doesn't hang or show spinners on normal reads
  - Verify refresh-price endpoint returns 502 when API unavailable

### STK-08: External API Failure Fallback
- **Requirement:** When external market API fails, use last-known stored prices
- **Implementation (already in place):**
  - `MarketDataService` catches fetch failures and retains cache
  - `@Scheduled` polling logs failures but doesn't crash
  - `PortfolioItemRepository.updateCurrentPrice()` only updates on successful fetch
  - Fallback behavior: show "last known price" tooltip without error to user
- **Testing:**
  - Stop market API service
  - Add new stock (should warn "Could not fetch market price, using provided price")
  - Browse holdings (should show stale prices with tooltip)
  - Refresh price (should return 502 error; UI shows "Refresh failed, showing last known price")

### STK-10: Stale-Data Messaging & Refresh Indicator
- **Implementation:**
  - Add optional `isStale: boolean` flag to `PortfolioItemResponse`
  - Determine stale if price age > 1 hour or fetch failed on last attempt
  - UI shows "⚠ Price is stale" badge if flag is true
  - Offer quick "Refresh All Prices" button on holdings table
- **Testing:**
  - Set market API fetch to fail for 1 hour window
  - Verify holdings show stale indicator
  - Click "Refresh All" and verify all prices update (or clear stale flag)

---

## Phase 3: Stock Growth History Chart (2 days)

**Goals:** Display historical stock growth in an interactive chart

### STK-11: History Data Model & Persistence
- **New Table: `portfolio_item_history`**
  ```sql
  CREATE TABLE portfolio_item_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_item_id BIGINT NOT NULL,
    as_of_date DATE NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    quantity DECIMAL(19,4),
    holding_value DECIMAL(19,4),
    is_stale BOOLEAN DEFAULT FALSE,
    source VARCHAR(20),  -- 'TRADE', 'REFRESH', 'SNAPSHOT'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_item_id) REFERENCES portfolio_item(id) ON DELETE CASCADE,
    INDEX (portfolio_item_id, as_of_date)
  );
  ```

- **Data Capture Strategy:**
  1. **Trade Events:** On buy/sell, insert history row with execution price + quantity
  2. **Periodic Snapshots:** Daily job captures end-of-day price for each holding (planned jobs)
  3. **Explicit Refresh:** On refresh-price call, insert history row with refreshed price

- **Repository Method:**
  ```java
  List<HistoryPoint> findHistoryBetween(Long itemId, LocalDate from, LocalDate to);
  // Returns: List<{date, price, holdingValue, isStale}>
  ```

### STK-12: History Endpoint & Contract
- **Endpoint:** `GET /api/v1/portfolio-items/{id}/history?range=1M|3M|1Y|ALL`
  - Default range: `ALL`
  - Response: ordered array of `{date, price, holdingValue}`
  - Example:
    ```json
    [
      { "date": "2026-06-01", "price": 140.25, "holdingValue": 1402.50 },
      { "date": "2026-06-15", "price": 150.00, "holdingValue": 1500.00 },
      { "date": "2026-07-01", "price": 155.50, "holdingValue": 1555.00 }
    ]
    ```

- **Service Method:**
  ```java
  public List<HistoryPoint> getStockHistory(Long itemId, String range) {
    LocalDate from = LocalDate.now().minus(parseDuration(range));
    return repository.findHistoryBetween(itemId, from, LocalDate.now());
  }
  ```

- **API Contract Update:**
  ```
  GET /api/v1/portfolio-items/{id}/history?range=1M|3M|1Y|ALL
  200 → [ { date, price, holdingValue }, ... ]
  404 → not found
  ```

### STK-13: History Chart UI & Range Selector
- **Frontend Addition:**
  - New `getPortfolioItemHistory(id, range)` in `api.js`
  - New `renderStockGrowthChart(canvasId, points)` in `charts.js` using Chart.js
  - Chart Type: Line chart (price trend) with selectable range buttons

- **Chart Placement:**
  - **Option 1:** New section in Stocks tab (selected holding → show chart below table)
  - **Option 2:** Standalone modal on per-row "Chart" button
  - **Option 3:** Dashboard secondary chart (portfolio performance, if data available)
  - *Recommended for Phase 3:* Option 1 (Stocks tab context)

- **Range Selector Buttons:** `1M | 3M | 1Y | ALL`
  - Click button → reload chart with new date range
  - Active button highlighted
  - Default: `ALL` (or `1Y` if ALL has too many points)

- **No-Data States:**
  - "Not enough history yet" if < 2 data points
  - "No price data available for this range" if range exists but is empty
  - Don't show chart if data unavailable

- **Error Handling:**
  - If history endpoint unavailable, show placeholder (don't crash)
  - If partial/stale data, render available points with "Last known data" badge
  - API failure doesn't block holdings table

- **Chart Config:**
  - X-axis: Date labels (auto-format: daily/weekly/monthly based on range)
  - Y-axis: Price (currency format, dynamic scale)
  - Line: Smooth curve, color from theme (gain=green, loss=red oscillating)
  - Tooltip: On hover → `date: price, value: {holding total}`
  - Responsive: Adapts to container size

- **Code Structure:**
  - New module: `src/main/resources/static/js/stockChart.js`
  - Export: `loadStockHistory(itemId, range)`, `renderChart(data)`, `updateChartRange(range)`
  - Wire to Stocks tab: on holding selection/click, load and render chart

### STK-14: Testing & Validation
- **Backend Tests:**
  - Unit: `PortfolioHistoryRepositoryTest` — insert, query by date range, ordering
  - Integration: `PortfolioItemServiceTest` — auto-insert on buy/sell/refresh, range filtering
  - Controller: `PortfolioSummaryControllerTest` — endpoint returns correct shape, handles not found

- **Frontend Tests (manual):**
  - Hold row with 3+ trades → click chart button → verify chart renders
  - Chart X-axis labels correct for date range
  - Range buttons switch data correctly (visual check)
  - No chart if < 2 points
  - API failure → no crash, fallback message shown
  - Resize browser → chart re-layouts

---

## Summary Timeline

| Phase | Story | Tasks | Est. Days | Start | End |
|-------|-------|-------|-----------|-------|-----|
| 1 | US-03/06/07/09/10/14 | STK-01 to STK-05 | 0.5 | Aug 5 | Aug 5 ✅ |
| 2 | US-10/US-13 | STK-06 to STK-10 | 1 | Aug 6 | Aug 6 |
| 3 | US-15 (Stocks) | STK-11 to STK-14 | 1.5 | Aug 7 | Aug 8 |
| **Total** | **Stocks MVP + History** | **14 tasks** | **3** | Aug 5 | Aug 8 |

---

## Acceptance Criteria Traceability (Phases 2-3)

| AC / US | Requirement | Phase | Status |
|---------|-------------|-------|--------|
| AC-09 / US-10 | Graceful External Data Failure | 2 | 🔄 In Phase 1 (basic), enhanced in Phase 2 |
| AC-10 / US-13 | Controlled Price Refresh | 2 | 🔄 Hardening phase |
| US-15 | Performance Over Time Chart (Stretch) | 3 | 📋 Planned |

---

## Dependencies & Blockers

- ✅ Phase 1 must complete first (CRUD, validation, error handling stable)
- ⚠️ Phase 2 blocks Phase 3 (history data depends on controlled refresh)
- ⚠️ Market API must be available for testing (mock available)

---

## Success Criteria

**Phase 2 Complete When:**
- Stocks tab never hangs on normal reads (cache-first proven)
- Explicit refresh endpoint works, returns 502 on API failure
- Stale-price indicator shows/updates correctly
- No lost data when external API down

**Phase 3 Complete When:**
- History chart renders for any holding with 2+ data points
- Range selector changes data visually correctly
- No-data states show graceful fallback
- API failure on history endpoint doesn't block holdings table

---

## Optional Enhancements (Post Phase 3)

1. **Portfolio Performance Graph:** Aggregate all holdings' gain/loss over time
2. **Alerts & Notifications:** Notify on price crosses (e.g., stock crosses $100)
3. **Trade History UI:** Show list of buy/sell trades with entry price, quantity, date
4. **Export Data:** CSV export of holdings + trade history
5. **Watchlist:** Save/track stocks without owning them

---

**Roadmap Owner:** Portfolio Team  
**Last Updated:** August 5, 2026  
**Next Milestone:** Phase 2 Kickoff Retro


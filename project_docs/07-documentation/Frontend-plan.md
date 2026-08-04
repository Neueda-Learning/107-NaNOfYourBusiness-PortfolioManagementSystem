# Frontend Plan — Portfolio Manager

> Give this file to Copilot as context before you start writing frontend code.
> It defines structure, pages, and UI behaviour for the HTML/CSS/JavaScript frontend.
> All API calls described here must match `API-contract.md` exactly.

## 1. Goals (in priority order, per the assignment)

1. Browse the portfolio (by asset type: Stocks / Mutual Funds / Bonds)
2. View portfolio performance graphically (dashboard with charts)
3. Add items to the portfolio
4. Remove items from the portfolio
5. (Stretch) Edit existing items, AI insights section

## 2. Assumptions (adjust if wrong)

- Plain HTML/CSS/JavaScript — **no framework** (no React/Vue/Angular), so keep JS modular using ES modules (`type="module"`) rather than one giant script file.
- No authentication/login screen — the app opens directly on the Dashboard.
- Served as static resources from `src/main/resources/static` in the same Spring Boot app (simplest option for a single-user training project — no separate frontend server or CORS setup needed). If you'd rather run a separate static server (e.g. Live Server on a different port), the only change needed is enabling CORS on the backend for that origin — flag this to your instructor if you go that route.
- Single page app-ish behaviour achieved via **tabs shown/hidden with JS**, not full page reloads — keeps state (like a loaded chart) intact and avoids re-fetching everything on every navigation.
- Charting library: **Chart.js** (via CDN) — simple, well-documented, no build step required.

## 3. Folder Structure

```
src/main/resources/static/
├── index.html                 # single entry point: header, tab nav, tab panels
├── css/
│   ├── variables.css          # colour palette, spacing, font tokens
│   ├── layout.css             # page shell, nav, grid/cards
│   └── components.css         # buttons, forms, modals, tables
├── js/
│   ├── api.js                 # fetch wrapper — single place that knows the API base URL
│   ├── app.js                 # entry point: wires up tab switching, initial load
│   ├── dashboard.js           # dashboard tab: summary cards + charts
│   ├── portfolioTab.js        # generic tab controller reused for Stocks/Bonds/Mutual Funds
│   ├── itemForm.js            # add/edit modal form logic + validation
│   └── charts.js              # Chart.js setup/config helpers
└── assets/
    └── (icons/images if needed)
```

Keep each JS file focused on one concern — this makes it much easier for Copilot to generate correct, isolated changes without touching unrelated code.

## 4. Pages / Views

Single `index.html`, structured as:

- **Header** — app title, maybe last-refreshed timestamp for stock prices.
- **Tab navigation** — `Dashboard | Stocks | Mutual Funds | Bonds`
- **Tab panels** (only one visible at a time, toggled via JS + a simple `hidden` attribute or CSS class):
  - **Dashboard panel**: summary cards (total value, total gain/loss, item count) + allocation chart (pie/doughnut by asset type) + a performance-over-time chart if the backend supports it.
  - **Stocks / Mutual Funds / Bonds panels**: each renders the same reusable table/list component, filtered by `type`, with:
    - "Add [Stock/Bond/Fund]" button → opens a modal form
    - Table rows showing key fields (symbol/name, quantity, purchase price, current price, gain/loss) with a **Remove** action per row (and Edit, if you get to it)

## 5. Component Plan

- **Tab nav**: plain buttons/links with `data-tab="stocks"` attributes; `app.js` listens for clicks and toggles panel visibility + triggers that panel's data load on first view (lazy-load, don't fetch all four tabs' data upfront).
- **Summary cards**: small reusable card markup (label + big number), populated from the dashboard summary endpoint.
- **Charts** (`charts.js`):
  - Allocation breakdown: doughnut chart, one segment per asset type.
  - Performance over time: line chart (stretch goal, depends on backend support).
- **Item table**: reusable render function `renderItemTable(containerEl, items)` shared across the three asset-type tabs — don't triplicate this logic.
- **Add/Edit modal** (`itemForm.js`): simple `<dialog>` element or a CSS-shown div; form fields driven by asset type (see `API-contract.md` for exact fields per type); client-side validation before `POST`/`PUT` (required fields, positive numbers, valid date).
- **Remove confirmation**: simple `confirm()` dialog is fine for a training project — no need for a custom modal here.
- **Loading / error states**: every panel should show a lightweight loading indicator while fetching and a plain error message if the fetch fails (don't leave the user looking at a blank table).

## 6. JavaScript Architecture

- `api.js` exports small functions per resource, e.g.:
  ```js
  const BASE_URL = "/api/v1";

  export async function getPortfolioItems(type) { ... }
  export async function getPortfolioItem(id) { ... }
  export async function createPortfolioItem(payload) { ... }
  export async function updatePortfolioItem(id, payload) { ... }
  export async function deletePortfolioItem(id) { ... }
  export async function getPortfolioSummary() { ... }
  ```
  Every other JS file calls through `api.js` — no `fetch()` calls scattered elsewhere. This is the main place that has to stay in sync with `API-contract.md`.
- Use `async/await` with `try/catch` around every `fetch` call; surface errors to the UI rather than only `console.error`.
- No global mutable state beyond what's needed to avoid redundant re-fetching (e.g. cache the currently loaded items per tab in a simple module-level object).
- Keep functions small and named descriptively — this keeps Copilot's autocomplete/suggestions relevant and scoped.

## 7. Styling Approach

- Clean, minimal, dashboard-like aesthetic: light background, card-based layout, generous whitespace, one accent colour for primary actions (Add/Save buttons) and a clear red/green convention for loss/gain figures.
- Define a small set of CSS custom properties in `variables.css` (colours, spacing scale, font) so the look stays consistent without a framework.
- Responsive enough for a laptop screen at minimum — full mobile support is not a priority for this assignment unless your instructor asks for it.

## 8. Data Flow Summary

1. On load, `app.js` shows the Dashboard tab and calls `getPortfolioSummary()` → renders summary cards + allocation chart.
2. Switching to Stocks/Mutual Funds/Bonds tab (first time) calls `getPortfolioItems(type)` → renders the table for that type; cached in memory for subsequent tab switches within the same session.
3. Submitting the Add form calls `createPortfolioItem(payload)`; on success, re-fetch that tab's items (and the dashboard summary, since totals changed) and close the modal.
4. Clicking Remove calls `deletePortfolioItem(id)` after confirmation; on success, remove the row from the table and refresh the dashboard summary.

## 9. Milestones Checklist

- [ ] Static page shell with working tab navigation (no data yet)
- [ ] `api.js` wired up against real backend CRUD endpoints
- [ ] Stocks/Mutual Funds/Bonds tabs list real data in tables
- [ ] Add item flow working end-to-end (form → POST → table updates)
- [ ] Remove item flow working end-to-end
- [ ] Dashboard summary cards populated from backend
- [ ] Allocation chart rendering via Chart.js
- [ ] Loading and error states handled on every panel
- [ ] (Stretch) Edit item flow, performance-over-time chart, AI insights panel

## 10. Notes for Using Copilot

- Keep `API-contract.md` open/pasted into context when asking Copilot to write or modify anything in `api.js` — field name mismatches between frontend and backend are the most common source of bugs on this kind of project.
- Ask Copilot for one component/file at a time (e.g. "write `renderItemTable` in `portfolioTab.js` per Frontend-plan.md §5") rather than "build the whole frontend," so you can review each piece.
- If Copilot suggests a framework (React, etc.) or a build step, decline unless you've explicitly decided to change the tech stack — this plan assumes plain HTML/CSS/JS with no build tooling.

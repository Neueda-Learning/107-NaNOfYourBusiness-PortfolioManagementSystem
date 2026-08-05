/**
 * dashboard.js — loads the Dashboard tab data and renders it.
 *
 * Responsibilities:
 *  - Fetch summary from backend via api.js
 *  - Populate the three summary cards (total value, gain/loss, item count)
 *  - Render the allocation doughnut chart via charts.js
 *  - Show loading, error, and empty states
 */

import { getPortfolioSummary } from "./api.js";
import { renderAllocationChart } from "./charts.js";

// Keep the latest allocation snapshot so we can re-render chart on theme toggle.
let _lastAllocationByType = [];

// ── Element references (set once on first load) ──────
const el = {
  loading:      () => document.getElementById("dashboard-loading"),
  error:        () => document.getElementById("dashboard-error"),
  errorMsg:     () => document.getElementById("dashboard-error-msg"),
  empty:        () => document.getElementById("dashboard-empty"),
  content:      () => document.getElementById("dashboard-content"),

  totalValue:   () => document.getElementById("card-total-value"),
  gainLoss:     () => document.getElementById("card-gain-loss"),
  gainLossPct:  () => document.getElementById("card-gain-loss-pct"),
  itemCount:    () => document.getElementById("card-item-count"),
};

/** Currency formatter */
const fmt = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

function show(id)  { const e = document.getElementById(id); if (e) e.style.display = ""; }
function hide(id)  { const e = document.getElementById(id); if (e) e.style.display = "none"; }

/** Entry point — called by app.js when the Dashboard tab is activated */
export async function loadDashboard() {
  // Reset state
  show("dashboard-loading");
  hide("dashboard-error");
  hide("dashboard-empty");
  hide("dashboard-content");

  try {
    const summary = await getPortfolioSummary();
    hide("dashboard-loading");
    renderSummaryCards(summary);

    const hasHoldings = summary.itemCount > 0;
    if (!hasHoldings) {
      show("dashboard-empty");
      return;
    }

    show("dashboard-content");
    _lastAllocationByType = summary.allocationByType ?? [];
    renderAllocationChart("allocationChart", _lastAllocationByType);
  } catch (err) {
    hide("dashboard-loading");
    const msgEl = el.errorMsg();
    if (msgEl) msgEl.textContent = err.message || "Failed to load portfolio data.";
    show("dashboard-error");
  }
}

// Repaint chart colours when the user toggles light/dark mode.
window.addEventListener("themechange", () => {
  const dashboardPanel = document.querySelector('.tab-panel[data-panel="dashboard"]');
  if (!dashboardPanel?.classList.contains("active")) return;
  if (!_lastAllocationByType.length) return;
  renderAllocationChart("allocationChart", _lastAllocationByType);
});

/** Populate the three summary metric cards */
function renderSummaryCards(summary) {
  const totalValueEl  = el.totalValue();
  const gainLossEl    = el.gainLoss();
  const gainLossPctEl = el.gainLossPct();
  const itemCountEl   = el.itemCount();

  if (totalValueEl) totalValueEl.textContent = fmt.format(summary.totalValue ?? 0);

  if (gainLossEl && gainLossPctEl) {
    const gl  = parseFloat(summary.totalGainLoss ?? 0);
    const pct = parseFloat(summary.totalGainLossPercent ?? 0);
    const sign = gl >= 0 ? "+" : "";
    gainLossEl.textContent    = `${sign}${fmt.format(gl)}`;
    gainLossPctEl.textContent = `${sign}${pct.toFixed(2)}%`;

    // Colour coding
    const cls = gl >= 0 ? "card__value--gain" : "card__value--loss";
    gainLossEl.className = `card__value ${cls}`;
  }

  if (itemCountEl) itemCountEl.textContent = summary.itemCount ?? 0;
}


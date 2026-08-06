/**
 * dashboard.js — loads the Dashboard tab data and renders it.
 *
 * Responsibilities:
 *  - Fetch summary from backend via api.js
 *  - Populate the summary cards (total value, gain/loss, holdings, cost basis)
 *  - Render the allocation doughnut chart via charts.js
 *  - Render sparklines (client-cached snapshot trend) per card
 *  - Render a diversification insight and a top-movers widget
 *  - Show loading (skeleton), error, and empty states
 */

import { getPortfolioSummary, getPortfolioItems, getPortfolioPerformance } from "./api.js";
import { renderAllocationChart, renderSparkline, renderPerformanceChart } from "./charts.js";

// Keep the latest allocation snapshot so we can re-render chart on theme toggle.
let _lastAllocationByType = [];
let _lastTotalValue = null;
let _lastPerformancePoints = [];
let _currentPerfRange = "ALL";

const HISTORY_KEY = "pm-dashboard-history";
const HISTORY_MAX = 20;

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
  totalCost:    () => document.getElementById("card-total-cost"),

  lastUpdated:  () => document.getElementById("dashboard-last-updated"),
  refreshBtn:   () => document.getElementById("dashboard-refresh-btn"),
  emptyCta:     () => document.getElementById("dashboard-empty-cta"),
  insight:      () => document.getElementById("dashboard-insight"),

  topMoversCard: () => document.getElementById("top-movers-card"),
  topMoversBody: () => document.getElementById("top-movers-body"),

  perfRangeGroup: () => document.getElementById("perf-range-group"),
  perfLoading:    () => document.getElementById("performance-loading"),
  perfEmpty:      () => document.getElementById("performance-empty"),
  perfCanvasWrap: () => document.getElementById("performance-canvas-wrap"),
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

  const refreshBtn = el.refreshBtn();
  if (refreshBtn) refreshBtn.disabled = true;

  try {
    const summary = await getPortfolioSummary();
    hide("dashboard-loading");
    if (refreshBtn) refreshBtn.disabled = false;

    const hasHoldings = summary.itemCount > 0;
    if (!hasHoldings) {
      show("dashboard-empty");
      return;
    }

    show("dashboard-content");
    _lastAllocationByType = summary.allocationByType ?? [];
    _lastTotalValue = summary.totalValue ?? 0;

    const history = recordSnapshot(summary);
    renderSummaryCards(summary, history);
    renderAllocationChart("allocationChart", _lastAllocationByType, _lastTotalValue);
    renderInsight(_lastAllocationByType);
    renderLastUpdated();
    loadTopMovers();
    loadPerformanceChart(_currentPerfRange);
  } catch (err) {
    hide("dashboard-loading");
    if (refreshBtn) refreshBtn.disabled = false;
    const msgEl = el.errorMsg();
    if (msgEl) msgEl.textContent = describeError(err);
    show("dashboard-error");
  }
}

/** Turn a caught error into a user-friendly, differentiated message (US-14 / US-10) */
function describeError(err) {
  // fetch() throws a plain TypeError ("Failed to fetch") when the network/server is unreachable.
  if (err instanceof TypeError) {
    return "Network problem — could not reach the server. Check your connection and retry.";
  }
  return err?.message || "Failed to load portfolio data. Please retry.";
}

// Repaint chart colours when the user toggles light/dark mode.
window.addEventListener("themechange", () => {
  const dashboardPanel = document.querySelector('.tab-panel[data-panel="dashboard"]');
  if (!dashboardPanel?.classList.contains("active")) return;
  if (_lastAllocationByType.length) {
    renderAllocationChart("allocationChart", _lastAllocationByType, _lastTotalValue);
  }
  if (_lastPerformancePoints.length) {
    renderPerformanceChart("performanceChart", _lastPerformancePoints);
  }
});

// Manual refresh button + empty-state CTA + performance range toggle
document.addEventListener("DOMContentLoaded", () => {
  el.refreshBtn()?.addEventListener("click", () => loadDashboard());
  el.emptyCta()?.addEventListener("click", () => {
    document.querySelector('.tab-nav__btn[data-tab="stocks"]')?.click();
  });

  el.perfRangeGroup()?.addEventListener("click", e => {
    const btn = e.target.closest(".perf-range-btn");
    if (!btn || btn.classList.contains("active")) return;
    el.perfRangeGroup().querySelectorAll(".perf-range-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    _currentPerfRange = btn.dataset.range;
    loadPerformanceChart(_currentPerfRange);
  });
});

/** Populate the summary metric cards, including count-up animation and sparklines */
function renderSummaryCards(summary, history) {
  const totalValueEl  = el.totalValue();
  const gainLossEl    = el.gainLoss();
  const gainLossPctEl = el.gainLossPct();
  const itemCountEl   = el.itemCount();
  const totalCostEl   = el.totalCost();

  const totalValue = parseFloat(summary.totalValue ?? 0);
  const gl  = parseFloat(summary.totalGainLoss ?? 0);
  const pct = parseFloat(summary.totalGainLossPercent ?? 0);
  const totalCost = parseFloat(summary.totalCost ?? 0);

  if (totalValueEl) animateNumber(totalValueEl, totalValue, fmt.format);

  if (gainLossEl && gainLossPctEl) {
    const sign = gl >= 0 ? "+" : "";
    animateNumber(gainLossEl, gl, v => `${sign}${fmt.format(v)}`);
    gainLossPctEl.textContent = `${sign}${pct.toFixed(2)}%`;

    // Colour coding
    const cls = gl >= 0 ? "card__value--gain" : "card__value--loss";
    gainLossEl.className = `card__value ${cls}`;

    const iconEl = document.getElementById("card-icon-gainloss");
    if (iconEl) iconEl.className = `card__icon ${gl >= 0 ? "" : "card__icon--loss"}`.trim();
  }

  if (itemCountEl) itemCountEl.textContent = summary.itemCount ?? 0;
  if (totalCostEl) animateNumber(totalCostEl, totalCost, fmt.format);

  // Sparklines from cached history (oldest -> newest)
  renderSparkline("sparkline-value", history.map(h => h.v), "neutral");
  renderSparkline("sparkline-gainloss", history.map(h => h.g), gl >= 0 ? "gain" : "loss");
}

/** Simple requestAnimationFrame count-up tween for a numeric card value */
function animateNumber(node, target, formatFn, duration = 600) {
  const startVal = parseFloat(node.dataset.rawValue || "0") || 0;
  // Skip animation if the value hasn't meaningfully changed
  if (Math.abs(startVal - target) < 0.01) {
    node.textContent = formatFn(target);
    node.dataset.rawValue = String(target);
    return;
  }
  const startTime = performance.now();
  function tick(now) {
    const t = Math.min(1, (now - startTime) / duration);
    // ease-out cubic
    const eased = 1 - Math.pow(1 - t, 3);
    const current = startVal + (target - startVal) * eased;
    node.textContent = formatFn(current);
    if (t < 1) requestAnimationFrame(tick);
    else node.dataset.rawValue = String(target);
  }
  requestAnimationFrame(tick);
}

/** Cache a lightweight snapshot of totals in localStorage for sparkline trend cues */
function recordSnapshot(summary) {
  let history = [];
  try {
    history = JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
  } catch (_) { history = []; }

  history.push({
    t: Date.now(),
    v: parseFloat(summary.totalValue ?? 0),
    g: parseFloat(summary.totalGainLoss ?? 0),
  });
  if (history.length > HISTORY_MAX) history = history.slice(-HISTORY_MAX);

  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(history)); } catch (_) { /* ignore quota errors */ }
  return history;
}

/** Show a "last updated" relative/absolute timestamp */
function renderLastUpdated() {
  const target = el.lastUpdated();
  if (!target) return;
  target.textContent = `Last updated ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
}

/** Diversification / concentration insight text under the allocation chart */
function renderInsight(allocationByType) {
  const target = el.insight();
  if (!target) return;
  if (!allocationByType.length) { target.textContent = ""; return; }

  const ASSET_LABELS = { STOCK: "Stocks", BOND: "Bonds", MUTUAL_FUND: "Mutual Funds" };
  const top = [...allocationByType].sort((a, b) => b.percent - a.percent)[0];
  const pct = parseFloat(top.percent);

  if (pct >= 60) {
    target.textContent =
      `${pct.toFixed(0)}% of your portfolio is concentrated in ${ASSET_LABELS[top.type] ?? top.type} — consider diversifying to manage risk.`;
  } else if (allocationByType.length >= 3) {
    target.textContent = "Your portfolio is reasonably diversified across all three asset types.";
  } else {
    target.textContent = `Currently holding ${allocationByType.length} of 3 asset types — add more variety to diversify further.`;
  }
}

/** Top Gainer / Top Loser widget, computed client-side from current holdings */
async function loadTopMovers() {
  const card = el.topMoversCard();
  const body = el.topMoversBody();
  if (!card || !body) return;

  try {
    const items = await getPortfolioItems();
    if (!items || items.length < 2) { card.style.display = "none"; return; }

    const withPct = items
      .map(i => ({ ...i, pct: parseFloat(i.gainLossPercent ?? 0) }))
      .filter(i => Number.isFinite(i.pct));
    if (withPct.length < 2) { card.style.display = "none"; return; }

    const gainer = withPct.reduce((a, b) => (b.pct > a.pct ? b : a));
    const loser  = withPct.reduce((a, b) => (b.pct < a.pct ? b : a));

    body.innerHTML = [
      moverHtml("Top Gainer", gainer, "gain"),
      moverHtml("Top Loser", loser, "loss"),
    ].join("");

    body.querySelectorAll("[data-jump-tab]").forEach(node => {
      node.addEventListener("click", () => {
        document.querySelector(`.tab-nav__btn[data-tab="${node.dataset.jumpTab}"]`)?.click();
      });
    });

    card.style.display = "";
  } catch (_) {
    // Non-critical widget — fail silently rather than breaking the dashboard.
    card.style.display = "none";
  }
}

function moverHtml(label, item, tone) {
  const tabMap = { STOCK: "stocks", BOND: "bonds", MUTUAL_FUND: "mutual-funds" };
  const tab = tabMap[item.type] ?? "dashboard";
  const sign = item.pct >= 0 ? "+" : "";
  return `
    <div class="top-mover top-mover--${tone}" data-jump-tab="${tab}" role="button" tabindex="0">
      <div>
        <p class="top-mover__label">${label}</p>
        <p class="top-mover__name">${escapeHtml(item.symbolOrName ?? "—")}</p>
      </div>
      <span class="top-mover__pct top-mover__pct--${tone}">${sign}${item.pct.toFixed(2)}%</span>
    </div>`;
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, c => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

/** Load and render the Performance Over Time chart for the given range (US-15) */
async function loadPerformanceChart(range) {
  const loading    = el.perfLoading();
  const empty      = el.perfEmpty();
  const canvasWrap = el.perfCanvasWrap();
  const rangeGroup = el.perfRangeGroup();

  if (!loading || !empty || !canvasWrap) return;

  hide("performance-empty");
  hide("performance-canvas-wrap");
  show("performance-loading");
  rangeGroup?.querySelectorAll(".perf-range-btn").forEach(b => (b.disabled = true));

  try {
    const response = await getPortfolioPerformance(range);
    const points = response?.points ?? [];
    hide("performance-loading");

    if (points.length < 2) {
      show("performance-empty");
      _lastPerformancePoints = [];
      return;
    }

    _lastPerformancePoints = points;
    show("performance-canvas-wrap");
    renderPerformanceChart("performanceChart", points);
  } catch (_) {
    // Non-critical chart — degrade to the empty state rather than breaking the dashboard (US-10).
    hide("performance-loading");
    show("performance-empty");
    _lastPerformancePoints = [];
  } finally {
    rangeGroup?.querySelectorAll(".perf-range-btn").forEach(b => (b.disabled = false));
  }
}


/**
 * mutualFunds.js — Mutual Funds tab
 *
 * Features:
 *  - Browse 30 supported mutual funds with live NAV from MFAPI
 *  - Buy mutual fund by amount (units calculated at current NAV)
 *  - Sell mutual fund by amount (units deducted at current NAV)
 *  - View current holdings with gain/loss
 */

import {
  getMutualFunds,
  buyMutualFund,
  sellMutualFund,
  getPortfolioItems,
  deletePortfolioItem,
  getMutualFundHistory,
  getMutualFundTransactions,
} from "./api.js?v=2";

let initialized = false;
let catalogue = [];       // { schemeCode, schemeName, latestNav }
let mfHoldings = [];      // portfolio items of type MUTUAL_FUND
let historyChart = null;
let currentHistorySchemeCode = null;
let currentViewSchemeCode = null; // fund currently shown in the detail/transaction panel

const byId = (id) => document.getElementById(id);

function fmtNum(val, decimals = 2) {
  if (val == null || isNaN(Number(val))) return "—";
  return Number(val).toLocaleString("en-IN", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

function fmtDate(val) {
  if (!val) return "—";
  const d = new Date(val);
  if (isNaN(d.getTime())) return String(val);
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function fmtCurrency(val) {
  if (val == null || isNaN(Number(val))) return "—";
  return "₹" + fmtNum(val, 2);
}

function setMsg(id, text, isError = false) {
  const el = byId(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = isError ? "var(--color-danger, #e53e3e)" : "var(--color-success, #38a169)";
}

// ── Catalogue ─────────────────────────────────────────

async function loadCatalogue() {
  const grid = byId("mf-catalogue-body");
  if (grid) grid.innerHTML = `<p class="holdings-table__empty">Loading NAV data from MFAPI…</p>`;
  setMsg("mf-catalogue-msg", "");

  try {
    catalogue = await getMutualFunds();
    renderCatalogue();
    populateBuyDropdown();
    populateViewDropdown();
  } catch (err) {
    setMsg("mf-catalogue-msg", "Failed to load catalogue: " + err.message, true);
    if (grid) grid.innerHTML = `<p class="holdings-table__empty">Error loading catalogue.</p>`;
  }
}

function renderCatalogue() {
  const grid = byId("mf-catalogue-body");
  if (!grid) return;

  if (!catalogue.length) {
    grid.innerHTML = `<p class="holdings-table__empty">No funds available.</p>`;
    return;
  }

  grid.innerHTML = catalogue.map(fund => {
    const nav = fund.latestNav != null
      ? fmtCurrency(fund.latestNav)
      : '<span class="mf-fund-chip__nav--empty">Unavailable</span>';
    return `
      <button type="button" class="mf-fund-chip" data-scheme-code="${fund.schemeCode}"
              onclick="window.__mfSelectFund(${fund.schemeCode})">
        <span class="mf-fund-chip__code">#${fund.schemeCode}</span>
        <span class="mf-fund-chip__name">${fund.schemeName}</span>
        <span class="mf-fund-chip__nav">${nav}</span>
        <span class="mf-fund-chip__history"
              onclick="event.stopPropagation(); window.__mfShowHistory(${fund.schemeCode}, '${fund.schemeName.replace(/'/g, "\\'")}', ${fund.latestNav ?? "null"})">
          History
        </span>
      </button>`;
  }).join("");
}

function populateBuyDropdown() {
  const sel = byId("mf-buy-scheme");
  if (!sel) return;
  sel.innerHTML = `<option value="">— Select a fund —</option>` +
    catalogue.map(f =>
      `<option value="${f.schemeCode}">${f.schemeName}${f.latestNav != null ? " (NAV: ₹" + fmtNum(f.latestNav) + ")" : ""}</option>`
    ).join("");
}

// Clicking "Select" in catalogue pre-fills the buy form and shows fund details
window.__mfSelectFund = function (schemeCode) {
  const sel = byId("mf-buy-scheme");
  if (sel) sel.value = String(schemeCode);
  const amtEl = byId("mf-buy-amount");
  if (amtEl) amtEl.focus();

  const viewSel = byId("mf-view-select");
  if (viewSel) viewSel.value = String(schemeCode);
  loadFundDetail(schemeCode);

  const catalogueWrap = byId("mf-catalogue-table-wrap");
  if (catalogueWrap) catalogueWrap.style.display = "none";
};

// ── Fund Detail + Per-Fund Transaction History ────────

function populateViewDropdown() {
  const sel = byId("mf-view-select");
  if (!sel) return;
  const previous = sel.value;
  sel.innerHTML = `<option value="">— Select a fund —</option>` +
    catalogue.map(f =>
      `<option value="${f.schemeCode}">${f.schemeName}${f.latestNav != null ? " (NAV: ₹" + fmtNum(f.latestNav) + ")" : ""}</option>`
    ).join("");
  if (previous) sel.value = previous;
}

async function loadFundDetail(schemeCode) {
  const panel = byId("mf-fund-detail");
  const txBody = byId("mf-fund-transactions-body");

  if (!schemeCode) {
    currentViewSchemeCode = null;
    if (panel) panel.style.display = "none";
    return;
  }

  currentViewSchemeCode = schemeCode;
  const fund = catalogue.find(f => String(f.schemeCode) === String(schemeCode));

  if (panel) panel.style.display = "block";
  const nameEl = byId("mf-fund-detail-name");
  const metaEl = byId("mf-fund-detail-meta");
  if (nameEl) nameEl.textContent = fund ? fund.schemeName : `Scheme ${schemeCode}`;
  if (metaEl) {
    metaEl.textContent = fund
      ? `Scheme Code: ${fund.schemeCode} · Latest NAV: ${fund.latestNav != null ? fmtCurrency(fund.latestNav) : "Unavailable"}`
      : `Scheme Code: ${schemeCode}`;
  }

  if (txBody) txBody.innerHTML = `<tr><td colspan="5" class="holdings-table__empty">Loading transaction history…</td></tr>`;

  try {
    const transactions = await getMutualFundTransactions(schemeCode);
    renderFundTransactions(transactions);
  } catch (err) {
    if (txBody) txBody.innerHTML = `<tr><td colspan="5" class="holdings-table__empty">Failed to load transaction history: ${err.message}</td></tr>`;
  }
}

function renderFundTransactions(transactions) {
  const txBody = byId("mf-fund-transactions-body");
  if (!txBody) return;

  if (!transactions || !transactions.length) {
    txBody.innerHTML = `<tr><td colspan="5" class="holdings-table__empty">No buy/sell transactions yet for this fund.</td></tr>`;
    return;
  }

  txBody.innerHTML = transactions.map(t => {
    const isBuy = t.side === "BUY";
    const color = isBuy ? "var(--color-gain)" : "var(--color-loss)";
    return `
      <tr>
        <td><span style="color:${color};font-weight:600;">${isBuy ? "Buy" : "Sell"}</span></td>
        <td>${fmtNum(t.units, 4)}</td>
        <td>${fmtCurrency(t.nav)}</td>
        <td>${fmtCurrency(t.amount)}</td>
        <td>${fmtDate(t.transactionDate)}</td>
      </tr>`;
  }).join("");
}

window.__mfViewHistoryForSelected = function () {
  if (currentViewSchemeCode == null) return;
  const fund = catalogue.find(f => String(f.schemeCode) === String(currentViewSchemeCode));
  window.__mfShowHistory(currentViewSchemeCode, fund ? fund.schemeName : `Scheme ${currentViewSchemeCode}`, fund?.latestNav ?? null);
};

// ── Holdings ──────────────────────────────────────────

async function loadHoldings() {
  const container = byId("mf-holdings-body");
  if (container) container.innerHTML = `<p class="holdings-table__empty">Loading…</p>`;
  setMsg("mf-holdings-msg", "");

  try {
    const all = await getPortfolioItems("MUTUAL_FUND");
    mfHoldings = Array.isArray(all) ? all : [];
    renderHoldings();
    populateSellDropdown();
  } catch (err) {
    setMsg("mf-holdings-msg", "Failed to load holdings: " + err.message, true);
    if (container) container.innerHTML = `<p class="holdings-table__empty">Error loading holdings.</p>`;
  }
}

function renderHoldings() {
  const container = byId("mf-holdings-body");
  if (!container) return;

  if (!mfHoldings.length) {
    container.innerHTML = `<p class="holdings-table__empty">No mutual fund holdings yet. Buy a fund to get started.</p>`;
    return;
  }

  container.innerHTML = mfHoldings.map(h => {
    const investedAmount = Number(h.quantity ?? 0) * Number(h.purchasePrice ?? 0);
    const gainLoss = h.gainLoss;
    const gainLossPct = h.gainLossPercent;

    let glValue = "—";
    let glCls = "";
    if (gainLoss != null) {
      const sign = Number(gainLoss) >= 0 ? "+" : "";
      glCls = Number(gainLoss) >= 0 ? "mf-holding-card__stat-value--gain" : "mf-holding-card__stat-value--loss";
      glValue = `${sign}${fmtCurrency(gainLoss)}${gainLossPct != null ? ` (${sign}${fmtNum(gainLossPct)}%)` : ""}`;
    }

    return `
      <div class="mf-holding-card">
        <div class="mf-holding-card__header">
          <div class="mf-holding-card__name">${h.symbolOrName}</div>
          <div class="mf-holding-card__date">Bought ${fmtDate(h.purchaseDate)}</div>
        </div>
        <div class="mf-holding-card__body">
          <div>
            <div class="mf-holding-card__stat-label">Units</div>
            <div class="mf-holding-card__stat-value">${fmtNum(h.quantity, 4)}</div>
          </div>
          <div>
            <div class="mf-holding-card__stat-label">Invested Amount</div>
            <div class="mf-holding-card__stat-value">${fmtCurrency(investedAmount)}</div>
          </div>
          <div>
            <div class="mf-holding-card__stat-label">Current Value</div>
            <div class="mf-holding-card__stat-value">${h.currentValue != null ? fmtCurrency(h.currentValue) : "—"}</div>
          </div>
          <div>
            <div class="mf-holding-card__stat-label">Gain / Loss</div>
            <div class="mf-holding-card__stat-value ${glCls}">${glValue}</div>
          </div>
        </div>
        <div class="mf-holding-card__footer">
          <button class="btn-secondary" style="font-size:0.75rem;padding:4px 10px;"
                  onclick="window.__mfSelectSell(${h.id}, '${h.symbolOrName.replace(/'/g, "\\'")}')">
            Sell
          </button>
        </div>
      </div>`;
  }).join("");
}

function populateSellDropdown() {
  const sel = byId("mf-sell-holding");
  if (!sel) return;
  sel.innerHTML = `<option value="">— Select a holding —</option>` +
    mfHoldings.map(h =>
      `<option value="${h.id}">${h.symbolOrName} (${fmtNum(h.quantity, 4)} units)</option>`
    ).join("");
}

// Clicking "Sell" in holdings pre-fills the sell form
window.__mfSelectSell = function (holdingId, name) {
  const sel = byId("mf-sell-holding");
  if (sel) sel.value = String(holdingId);
  const amtEl = byId("mf-sell-amount");
  if (amtEl) amtEl.focus();
};

// ── Buy ───────────────────────────────────────────────

async function handleBuy() {
  const schemeCode = parseInt(byId("mf-buy-scheme")?.value, 10);
  const amount = parseFloat(byId("mf-buy-amount")?.value);

  if (!schemeCode) { setMsg("mf-buy-result", "Please select a fund.", true); return; }
  if (!amount || amount <= 0) { setMsg("mf-buy-result", "Please enter a valid amount greater than 0.", true); return; }

  const btn = byId("mf-buy-btn");
  if (btn) { btn.disabled = true; btn.textContent = "Buying…"; }
  setMsg("mf-buy-result", "");

  try {
    // Buying date is set automatically to today's date on the server — no user input needed.
    const result = await buyMutualFund({ schemeCode, amount });
    setMsg("mf-buy-result",
      `✓ Bought ${fmtNum(result.units, 4)} units of ${result.schemeName} at NAV ₹${fmtNum(result.nav)} on ${fmtDate(result.purchaseDate)}`
    );
    // Reset form
    if (byId("mf-buy-scheme")) byId("mf-buy-scheme").value = "";
    if (byId("mf-buy-amount")) byId("mf-buy-amount").value = "";
    // Refresh holdings
    await loadHoldings();
    // Keep the fund-detail transaction history in sync if this fund is currently being viewed
    if (currentViewSchemeCode != null && String(currentViewSchemeCode) === String(schemeCode)) {
      await loadFundDetail(currentViewSchemeCode);
    }
    window.__markDashboardStale?.();
  } catch (err) {
    setMsg("mf-buy-result", "✗ " + err.message, true);
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = "Buy Fund"; }
  }
}

// ── Sell ──────────────────────────────────────────────

async function handleSell() {
  const portfolioItemId = parseInt(byId("mf-sell-holding")?.value, 10);
  const amount = parseFloat(byId("mf-sell-amount")?.value);

  if (!portfolioItemId) { setMsg("mf-sell-result", "Please select a holding.", true); return; }
  if (!amount || amount <= 0) { setMsg("mf-sell-result", "Please enter a valid amount greater than 0.", true); return; }

  const btn = byId("mf-sell-btn");
  if (btn) { btn.disabled = true; btn.textContent = "Selling…"; }
  setMsg("mf-sell-result", "");

  try {
    const result = await sellMutualFund({ portfolioItemId, amount });
    if (result.message === "Mutual fund holding closed") {
      setMsg("mf-sell-result", `✓ Holding fully closed. All units sold.`);
    } else {
      setMsg("mf-sell-result",
        `✓ Sold ${fmtNum(result.unitsSold, 4)} units at NAV ₹${fmtNum(result.nav)}. Remaining: ${fmtNum(result.remainingUnits, 4)} units`
      );
    }
    // Reset form
    if (byId("mf-sell-holding")) byId("mf-sell-holding").value = "";
    if (byId("mf-sell-amount")) byId("mf-sell-amount").value = "";
    // Refresh holdings
    await loadHoldings();
    // Keep the fund-detail transaction history in sync if this fund is currently being viewed
    if (currentViewSchemeCode != null) {
      await loadFundDetail(currentViewSchemeCode);
    }
    window.__markDashboardStale?.();
  } catch (err) {
    setMsg("mf-sell-result", "✗ " + err.message, true);
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = "Sell Fund"; }
  }
}

// ── NAV History Chart ─────────────────────────────────

async function renderHistoryChart(schemeCode, range) {
  const loadingEl = byId("mf-history-loading");
  const emptyEl = byId("mf-history-empty");
  const canvas = byId("mf-history-chart");

  if (loadingEl) loadingEl.style.display = "flex";
  if (emptyEl) emptyEl.style.display = "none";
  if (canvas) canvas.style.display = "none";

  try {
    const data = await getMutualFundHistory(schemeCode, range);
    const points = data.history || [];

    if (loadingEl) loadingEl.style.display = "none";

    if (!points.length) {
      if (emptyEl) emptyEl.style.display = "flex";
      return;
    }

    if (canvas) canvas.style.display = "block";

    const labels = points.map(p => p.date);
    const navValues = points.map(p => Number(p.nav));

    if (historyChart) {
      historyChart.destroy();
      historyChart = null;
    }

    const ctx = canvas.getContext("2d");
    const accentColor = getComputedStyle(document.documentElement)
      .getPropertyValue("--color-mutual-fund").trim() || "#059669";

    historyChart = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets: [{
          label: "NAV (₹)",
          data: navValues,
          borderColor: accentColor,
          backgroundColor: accentColor + "22",
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 4,
          fill: true,
          tension: 0.25,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        scales: {
          x: {
            ticks: { maxTicksLimit: 8, color: getComputedStyle(document.documentElement).getPropertyValue("--color-text-faint") },
            grid: { display: false },
          },
          y: {
            ticks: { color: getComputedStyle(document.documentElement).getPropertyValue("--color-text-faint") },
            grid: { color: getComputedStyle(document.documentElement).getPropertyValue("--color-border") },
          },
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => `NAV: ₹${fmtNum(ctx.parsed.y, 4)}`,
            },
          },
        },
      },
    });

    // Update subtitle with change over the period
    const first = navValues[0];
    const last = navValues[navValues.length - 1];
    const change = last - first;
    const changePct = first !== 0 ? (change / first) * 100 : 0;
    const sign = change >= 0 ? "+" : "";
    const subtitleEl = byId("mf-history-subtitle");
    if (subtitleEl) {
      subtitleEl.innerHTML = `Latest NAV: ₹${fmtNum(last, 4)} &nbsp;•&nbsp; ` +
        `<span style="color:${change >= 0 ? 'var(--color-success,#38a169)' : 'var(--color-danger,#e53e3e)'}">` +
        `${sign}₹${fmtNum(Math.abs(change), 4)} (${sign}${fmtNum(changePct, 2)}%)</span> over selected range`;
    }
  } catch (err) {
    if (loadingEl) loadingEl.style.display = "none";
    if (emptyEl) {
      emptyEl.style.display = "flex";
      emptyEl.querySelector(".empty-state__text").textContent = "Failed to load history: " + err.message;
    }
  }
}

window.__mfShowHistory = function (schemeCode, schemeName, latestNav) {
  currentHistorySchemeCode = schemeCode;
  const modal = byId("mf-history-modal");
  const titleEl = byId("mf-history-title");
  if (titleEl) titleEl.textContent = schemeName;
  if (modal) modal.style.display = "flex";

  // Reset range buttons to "ALL"
  document.querySelectorAll(".mf-range-btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.range === "ALL");
  });

  renderHistoryChart(schemeCode, "ALL");
};

window.__mfCloseHistory = function () {
  const modal = byId("mf-history-modal");
  if (modal) modal.style.display = "none";
  if (historyChart) {
    historyChart.destroy();
    historyChart = null;
  }
  currentHistorySchemeCode = null;
};

function wireHistoryRangeButtons() {
  document.querySelectorAll(".mf-range-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".mf-range-btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      if (currentHistorySchemeCode != null) {
        renderHistoryChart(currentHistorySchemeCode, btn.dataset.range);
      }
    });
  });
}

// ── Bootstrap ─────────────────────────────────────────

export async function loadMutualFunds() {
  if (initialized) {
    // Re-entering the tab — refresh holdings only (catalogue NAV is slow)
    await loadHoldings();
    return;
  }
  initialized = true;


  // Wire buttons
  byId("mf-buy-btn")?.addEventListener("click", handleBuy);
  byId("mf-sell-btn")?.addEventListener("click", handleSell);
  byId("mf-refresh-catalogue-btn")?.addEventListener("click", async () => {
    setMsg("mf-catalogue-msg", "Refreshing NAV data…");
    await loadCatalogue();
  });
  byId("mf-refresh-holdings-btn")?.addEventListener("click", loadHoldings);

  // Catalogue table is hidden by default — reveal it when the user opens the
  // dropdown, and hide it again once a fund has been picked.
  const viewSelect = byId("mf-view-select");
  const catalogueWrap = byId("mf-catalogue-table-wrap");
  const showCatalogue = () => { if (catalogueWrap) catalogueWrap.style.display = ""; };
  const hideCatalogue = () => { if (catalogueWrap) catalogueWrap.style.display = "none"; };

  viewSelect?.addEventListener("mousedown", showCatalogue); // click to open (desktop)
  viewSelect?.addEventListener("focus", showCatalogue);     // keyboard/tab focus

  viewSelect?.addEventListener("change", (e) => {
    const schemeCode = e.target.value ? parseInt(e.target.value, 10) : null;
    loadFundDetail(schemeCode);
    if (schemeCode) hideCatalogue();
    else showCatalogue();
  });
  byId("mf-fund-detail-history-btn")?.addEventListener("click", () => {
    window.__mfViewHistoryForSelected();
  });
  wireHistoryRangeButtons();

  // Load data
  await Promise.all([loadCatalogue(), loadHoldings()]);
}



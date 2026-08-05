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
} from "./api.js";

let initialized = false;
let catalogue = [];       // { schemeCode, schemeName, latestNav }
let mfHoldings = [];      // portfolio items of type MUTUAL_FUND

const byId = (id) => document.getElementById(id);

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function fmtNum(val, decimals = 2) {
  if (val == null || isNaN(Number(val))) return "—";
  return Number(val).toLocaleString("en-IN", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
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
  const tbody = byId("mf-catalogue-body");
  if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="holdings-table__empty">Loading NAV data from MFAPI…</td></tr>`;
  setMsg("mf-catalogue-msg", "");

  try {
    catalogue = await getMutualFunds();
    renderCatalogue();
    populateBuyDropdown();
  } catch (err) {
    setMsg("mf-catalogue-msg", "Failed to load catalogue: " + err.message, true);
    if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="holdings-table__empty">Error loading catalogue.</td></tr>`;
  }
}

function renderCatalogue() {
  const tbody = byId("mf-catalogue-body");
  if (!tbody) return;

  if (!catalogue.length) {
    tbody.innerHTML = `<tr><td colspan="4" class="holdings-table__empty">No funds available.</td></tr>`;
    return;
  }

  tbody.innerHTML = catalogue.map(fund => {
    const nav = fund.latestNav != null ? fmtCurrency(fund.latestNav) : '<span style="color:var(--color-muted,#999)">Unavailable</span>';
    return `
      <tr>
        <td>${fund.schemeCode}</td>
        <td>${fund.schemeName}</td>
        <td>${nav}</td>
        <td>
          <button class="btn-secondary" style="font-size:0.75rem;padding:4px 10px;"
                  onclick="window.__mfSelectFund(${fund.schemeCode})">
            Select
          </button>
        </td>
      </tr>`;
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

// Clicking "Select" in catalogue pre-fills the buy form
window.__mfSelectFund = function (schemeCode) {
  const sel = byId("mf-buy-scheme");
  if (sel) sel.value = String(schemeCode);
  const amtEl = byId("mf-buy-amount");
  if (amtEl) amtEl.focus();
};

// ── Holdings ──────────────────────────────────────────

async function loadHoldings() {
  const tbody = byId("mf-holdings-body");
  if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="holdings-table__empty">Loading…</td></tr>`;
  setMsg("mf-holdings-msg", "");

  try {
    const all = await getPortfolioItems("MUTUAL_FUND");
    mfHoldings = Array.isArray(all) ? all : [];
    renderHoldings();
    populateSellDropdown();
  } catch (err) {
    setMsg("mf-holdings-msg", "Failed to load holdings: " + err.message, true);
    if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="holdings-table__empty">Error loading holdings.</td></tr>`;
  }
}

function renderHoldings() {
  const tbody = byId("mf-holdings-body");
  if (!tbody) return;

  if (!mfHoldings.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="holdings-table__empty">No mutual fund holdings yet. Buy a fund to get started.</td></tr>`;
    return;
  }

  tbody.innerHTML = mfHoldings.map(h => {
    const gainLoss = h.gainLoss;
    const gainLossPct = h.gainLossPercent;
    let glHtml = "—";
    if (gainLoss != null) {
      const sign = Number(gainLoss) >= 0 ? "+" : "";
      const color = Number(gainLoss) >= 0 ? "var(--color-success,#38a169)" : "var(--color-danger,#e53e3e)";
      glHtml = `<span style="color:${color}">${sign}${fmtCurrency(gainLoss)}</span>`;
      if (gainLossPct != null) {
        glHtml += `<br><small style="color:${color}">${sign}${fmtNum(gainLossPct)}%</small>`;
      }
    }

    return `
      <tr>
        <td>${h.symbolOrName}</td>
        <td>${fmtNum(h.quantity, 4)}</td>
        <td>${fmtCurrency(h.purchasePrice)}</td>
        <td>${h.currentPrice != null ? fmtCurrency(h.currentPrice) : "—"}</td>
        <td>${h.currentValue != null ? fmtCurrency(h.currentValue) : "—"}</td>
        <td>${glHtml}</td>
        <td>
          <button class="btn-secondary" style="font-size:0.75rem;padding:4px 10px;"
                  onclick="window.__mfSelectSell(${h.id}, '${h.symbolOrName.replace(/'/g, "\\'")}')">
            Sell
          </button>
        </td>
      </tr>`;
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
  const purchaseDate = byId("mf-buy-date")?.value || todayISO();

  if (!schemeCode) { setMsg("mf-buy-result", "Please select a fund.", true); return; }
  if (!amount || amount <= 0) { setMsg("mf-buy-result", "Please enter a valid amount greater than 0.", true); return; }

  const btn = byId("mf-buy-btn");
  if (btn) { btn.disabled = true; btn.textContent = "Buying…"; }
  setMsg("mf-buy-result", "");

  try {
    const result = await buyMutualFund({ schemeCode, amount, purchaseDate });
    setMsg("mf-buy-result",
      `✓ Bought ${fmtNum(result.units, 4)} units of ${result.schemeName} at NAV ₹${fmtNum(result.nav)}`
    );
    // Reset form
    if (byId("mf-buy-scheme")) byId("mf-buy-scheme").value = "";
    if (byId("mf-buy-amount")) byId("mf-buy-amount").value = "";
    if (byId("mf-buy-date")) byId("mf-buy-date").value = "";
    // Refresh holdings
    await loadHoldings();
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
    window.__markDashboardStale?.();
  } catch (err) {
    setMsg("mf-sell-result", "✗ " + err.message, true);
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = "Sell Fund"; }
  }
}

// ── Bootstrap ─────────────────────────────────────────

export async function loadMutualFunds() {
  if (initialized) {
    // Re-entering the tab — refresh holdings only (catalogue NAV is slow)
    await loadHoldings();
    return;
  }
  initialized = true;

  // Set default date to today
  const dateEl = byId("mf-buy-date");
  if (dateEl) dateEl.value = todayISO();

  // Wire buttons
  byId("mf-buy-btn")?.addEventListener("click", handleBuy);
  byId("mf-sell-btn")?.addEventListener("click", handleSell);
  byId("mf-refresh-catalogue-btn")?.addEventListener("click", async () => {
    setMsg("mf-catalogue-msg", "Refreshing NAV data…");
    await loadCatalogue();
  });
  byId("mf-refresh-holdings-btn")?.addEventListener("click", loadHoldings);

  // Load data
  await Promise.all([loadCatalogue(), loadHoldings()]);
}


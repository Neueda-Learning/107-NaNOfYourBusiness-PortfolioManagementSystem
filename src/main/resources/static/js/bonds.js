/**
 * bonds.js — Bonds tab
 *
 * Features:
 *  - Buy a bond (manual entry: essential fields required, rest optional)
 *  - View current bond holdings with gain/loss
 *  - Redeem a matured bond (button enabled only once maturityDate has passed)
 *  - View redemption history (previously redeemed bonds)
 */

import { getBonds, getRedeemedBonds, getBondCatalog, buyBond, redeemBond } from "./api.js";

let initialized = false;
let holdings = [];   // active bond holdings
let history = [];    // redeemed bonds
let catalog = [];    // distinct bonds (by symbol) available to preset the buy form from
let catalogCollapsed = true;
let catalogAutoExpanded = false;

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
  return "$" + fmtNum(val, 2);
}

function setMsg(id, text, isError = false) {
  const el = byId(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = isError ? "var(--color-danger, #e53e3e)" : "var(--color-success, #38a169)";
}

function isMatured(maturityDate) {
  if (!maturityDate) return false;
  return maturityDate <= todayISO();
}

// ── Holdings ──────────────────────────────────────────

async function loadHoldings() {
  const tbody = byId("bond-holdings-body");
  if (tbody) tbody.innerHTML = `<tr><td colspan="10" class="holdings-table__empty">Loading…</td></tr>`;
  setMsg("bond-holdings-msg", "");

  try {
    const all = await getBonds();
    holdings = Array.isArray(all) ? all : [];
    renderHoldings();
  } catch (err) {
    setMsg("bond-holdings-msg", "Failed to load holdings: " + err.message, true);
    if (tbody) tbody.innerHTML = `<tr><td colspan="10" class="holdings-table__empty">Error loading holdings.</td></tr>`;
  }
}

function renderHoldings() {
  const tbody = byId("bond-holdings-body");
  if (!tbody) return;

  if (!holdings.length) {
    tbody.innerHTML = `<tr><td colspan="10" class="holdings-table__empty">No bond holdings yet. Buy a bond to get started.</td></tr>`;
    return;
  }

  tbody.innerHTML = holdings.map(h => {
    const gainLoss = h.gainLoss;
    let glHtml = "—";
    if (gainLoss != null) {
      const sign = Number(gainLoss) >= 0 ? "+" : "";
      const color = Number(gainLoss) >= 0 ? "var(--color-success,#38a169)" : "var(--color-danger,#e53e3e)";
      glHtml = `<span style="color:${color}">${sign}${fmtCurrency(gainLoss)}</span>`;
    }

    const matured = isMatured(h.maturityDate);
    const redeemBtn = matured
      ? `<button class="btn-secondary" style="font-size:0.75rem;padding:4px 10px;"
                 onclick="window.__bondRedeem('${h.symbol.replace(/'/g, "\\'")}')">
           Redeem
         </button>`
      : `<button class="btn-secondary" style="font-size:0.75rem;padding:4px 10px;" disabled
                 title="Matures on ${h.maturityDate || '—'}">
           Redeem
         </button>`;

    return `
      <tr>
        <td>${h.symbol}</td>
        <td>${h.issuer || "—"}</td>
        <td>${fmtNum(h.quantity, 4)}</td>
        <td>${fmtCurrency(h.purchasePrice)}</td>
        <td>${h.currentPrice != null ? fmtCurrency(h.currentPrice) : "—"}</td>
        <td>${h.maturityDate || "—"}</td>
        <td>${h.creditRating || "—"}</td>
        <td>${h.currentValue != null ? fmtCurrency(h.currentValue) : "—"}</td>
        <td>${glHtml}</td>
        <td>${redeemBtn}</td>
      </tr>`;
  }).join("");
}

// ── Available Bonds catalog (own fetch from GET /api/v1/bonds; click a card to preset the Buy form) ──

async function loadCatalog() {
  const body = byId("bond-catalog-body");
  if (body) body.innerHTML = '<p style="color:var(--color-text-faint);font-size:var(--font-size-sm);padding:var(--space-2) 0;">Loading…</p>';

  try {
    const all = await getBondCatalog();
    const list = Array.isArray(all) ? all : [];
    const seen = new Set();
    catalog = list.filter((b) => {
      if (!b.symbol || seen.has(b.symbol)) return false;
      seen.add(b.symbol);
      return true;
    });

    // Show cards the first time we successfully load catalog entries.
    if (catalog.length && !catalogAutoExpanded) {
      setCatalogCollapsed(false);
      catalogAutoExpanded = true;
    }

    renderCatalog();
  } catch (err) {
    catalog = [];
    if (body) {
      body.innerHTML = "";
      const p = document.createElement("p");
      p.style.cssText = "color:var(--color-error-text,#e53e3e);font-size:var(--font-size-sm);padding:var(--space-2) 0;";
      p.textContent = `Could not load available bonds: ${err.message}`;
      body.appendChild(p);
    }
  }
}

function toggleCatalogCollapse() {
  setCatalogCollapsed(!catalogCollapsed);
}

function setCatalogCollapsed(collapsed) {
  catalogCollapsed = collapsed;
  const wrap = byId("bond-catalog-collapsible");
  const btn = byId("bond-catalog-toggle-btn");
  if (wrap) wrap.classList.toggle("is-collapsed", catalogCollapsed);
  if (btn) btn.setAttribute("aria-expanded", String(!catalogCollapsed));
}

function renderCatalog() {
  const body = byId("bond-catalog-body");
  if (!body) return;

  if (!catalog.length) {
    body.innerHTML = '<p style="color:var(--color-text-faint);font-size:var(--font-size-sm);padding:var(--space-2) 0;">No bonds available yet. Buy a bond to add it here.</p>';
    return;
  }

  body.innerHTML = "";
  catalog.forEach((b) => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "featured-ticker-chip bond-catalog-chip";
    chip.title = `Preset Buy Bond form from ${b.symbol}`;
    const redeemedBadge = b.status === "REDEEMED"
      ? `<span class="bond-catalog-chip__rating" style="background:var(--color-bg-secondary);color:var(--color-text-faint);">Redeemed</span>`
      : "";
    chip.innerHTML = `
      <span class="featured-chip__symbol">${b.symbol}</span>
      <span class="bond-catalog-chip__issuer">${b.issuer || "—"}</span>
      <span class="bond-catalog-chip__meta">${b.couponRate != null ? fmtNum(b.couponRate, 2) + "% · " : ""}${b.maturityDate || "—"}</span>
      ${b.creditRating ? `<span class="bond-catalog-chip__rating">${b.creditRating}</span>` : ""}
      ${redeemedBadge}
    `;
    chip.addEventListener("click", () => applyCatalogPreset(b));
    body.appendChild(chip);
  });
}

function applyCatalogPreset(b) {
  if (byId("bond-symbol")) byId("bond-symbol").value = b.symbol || "";
  if (byId("bond-maturity-date")) byId("bond-maturity-date").value = b.maturityDate || "";
  if (byId("bond-purchase-price")) byId("bond-purchase-price").value = b.faceValue ?? "";
  if (byId("bond-issuer")) byId("bond-issuer").value = b.issuer || "";
  if (byId("bond-face-value")) byId("bond-face-value").value = b.faceValue ?? "";
  if (byId("bond-coupon-rate")) byId("bond-coupon-rate").value = b.couponRate ?? "";
  if (byId("bond-coupon-frequency")) byId("bond-coupon-frequency").value = b.couponFrequency || "";
  if (byId("bond-credit-rating")) byId("bond-credit-rating").value = b.creditRating || "";
  if (byId("bond-yield-rate")) byId("bond-yield-rate").value = b.yieldRate ?? "";

  // Reveal the advanced fields since we just filled several of them
  const panel = byId("bond-advanced-fields");
  const toggleBtn = byId("bond-advanced-toggle");
  if (panel && panel.style.display === "none") {
    panel.style.display = "block";
    toggleBtn?.setAttribute("aria-expanded", "true");
    if (toggleBtn) toggleBtn.textContent = "− Optional details (issuer, coupon, rating…)";
  }

  setMsg("bond-buy-result", `Preset details from ${b.symbol}. Adjust quantity/price and click Buy.`);
  byId("bond-quantity")?.focus();
}

// Redeem action triggered from a holdings row button
window.__bondRedeem = async function (symbol) {
  setMsg("bond-holdings-msg", "");
  try {
    const result = await redeemBond(symbol);
    setMsg("bond-holdings-msg",
      `✓ Redeemed ${result.symbol} for ${fmtCurrency(result.redemptionValue)}`
    );
    await Promise.all([loadHoldings(), loadHistory(), loadCatalog()]);
    window.__markDashboardStale?.();
  } catch (err) {
    if (err.errorCode === "BOND_NOT_MATURED") {
      setMsg("bond-holdings-msg", `⏰ ${err.message}`, true);
      return;
    }
    if (err.errorCode === "BOND_ALREADY_REDEEMED") {
      setMsg("bond-holdings-msg", `ℹ ${err.message}`, true);
      return;
    }
    setMsg("bond-holdings-msg", "✗ " + err.message, true);
  }
};

// ── Redemption History ───────────────────────────────

async function loadHistory() {
  const tbody = byId("bond-history-body");
  if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">Loading…</td></tr>`;
  setMsg("bond-history-msg", "");

  try {
    const all = await getRedeemedBonds();
    history = Array.isArray(all) ? all : [];
    renderHistory();
  } catch (err) {
    setMsg("bond-history-msg", "Failed to load redemption history: " + err.message, true);
    if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">Error loading history.</td></tr>`;
  }
}

function renderHistory() {
  const tbody = byId("bond-history-body");
  if (!tbody) return;

  if (!history.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">No redeemed bonds yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = history.map(h => `
      <tr>
        <td>${h.symbol}</td>
        <td>${h.issuer || "—"}</td>
        <td>${fmtNum(h.quantity, 4)}</td>
        <td>${h.faceValue != null ? fmtCurrency(h.faceValue) : "—"}</td>
        <td>${h.redemptionDate || "—"}</td>
        <td>${h.redemptionValue != null ? fmtCurrency(h.redemptionValue) : "—"}</td>
      </tr>`).join("");
}

// ── Buy ───────────────────────────────────────────────

function toggleAdvanced() {
  const panel = byId("bond-advanced-fields");
  const btn = byId("bond-advanced-toggle");
  if (!panel || !btn) return;
  const expanded = panel.style.display !== "none";
  panel.style.display = expanded ? "none" : "block";
  btn.setAttribute("aria-expanded", String(!expanded));
  btn.textContent = expanded ? "+ Optional details (issuer, coupon, rating…)" : "− Optional details (issuer, coupon, rating…)";
}

function numOrNull(id) {
  const raw = byId(id)?.value;
  if (raw === undefined || raw === null || raw === "") return null;
  const num = parseFloat(raw);
  return isNaN(num) ? null : num;
}

function textOrNull(id) {
  const raw = byId(id)?.value?.trim();
  return raw ? raw : null;
}

async function handleBuy() {
  const symbol = byId("bond-symbol")?.value?.trim();
  const quantity = parseFloat(byId("bond-quantity")?.value);
  const purchasePrice = parseFloat(byId("bond-purchase-price")?.value);
  const purchaseDate = byId("bond-purchase-date")?.value || todayISO();
  const maturityDate = byId("bond-maturity-date")?.value || null;

  if (!symbol) { setMsg("bond-buy-result", "Please enter a symbol.", true); return; }
  if (!quantity || quantity <= 0) { setMsg("bond-buy-result", "Please enter a valid quantity greater than 0.", true); return; }
  if (!purchasePrice || purchasePrice <= 0) { setMsg("bond-buy-result", "Please enter a valid purchase price greater than 0.", true); return; }

  const payload = {
    symbol,
    quantity,
    purchasePrice,
    purchaseDate,
    currentPrice: numOrNull("bond-current-price"),
    issuer: textOrNull("bond-issuer"),
    faceValue: numOrNull("bond-face-value"),
    couponRate: numOrNull("bond-coupon-rate"),
    couponFrequency: textOrNull("bond-coupon-frequency"),
    maturityDate,
    creditRating: textOrNull("bond-credit-rating"),
    yieldRate: numOrNull("bond-yield-rate"),
  };

  const btn = byId("bond-buy-btn");
  if (btn) { btn.disabled = true; btn.textContent = "Buying…"; }
  setMsg("bond-buy-result", "");

  try {
    const result = await buyBond(payload);
    setMsg("bond-buy-result",
      `✓ Bought ${fmtNum(result.quantity, 4)} units of ${result.symbol} at ${fmtCurrency(result.purchasePrice)}`
    );
    // Reset essential fields
    ["bond-symbol", "bond-quantity", "bond-purchase-price", "bond-maturity-date",
     "bond-issuer", "bond-current-price", "bond-face-value", "bond-coupon-rate",
     "bond-credit-rating", "bond-yield-rate"].forEach(id => { if (byId(id)) byId(id).value = ""; });
    if (byId("bond-coupon-frequency")) byId("bond-coupon-frequency").value = "";
    if (byId("bond-purchase-date")) byId("bond-purchase-date").value = todayISO();

    await Promise.all([loadHoldings(), loadCatalog()]);
    window.__markDashboardStale?.();
  } catch (err) {
    setMsg("bond-buy-result", "✗ " + err.message, true);
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = "Buy Bond"; }
  }
}

// ── Bootstrap ─────────────────────────────────────────

export async function loadBonds() {
  if (initialized) {
    await Promise.all([loadHoldings(), loadHistory(), loadCatalog()]);
    return;
  }
  initialized = true;

  // Set default purchase date to today
  const dateEl = byId("bond-purchase-date");
  if (dateEl) dateEl.value = todayISO();

  // Wire buttons
  byId("bond-buy-btn")?.addEventListener("click", handleBuy);
  byId("bond-advanced-toggle")?.addEventListener("click", toggleAdvanced);
  byId("bond-refresh-holdings-btn")?.addEventListener("click", loadHoldings);
  byId("bond-refresh-history-btn")?.addEventListener("click", loadHistory);
  byId("bond-catalog-toggle-btn")?.addEventListener("click", toggleCatalogCollapse);
  byId("bond-catalog-refresh-btn")?.addEventListener("click", loadCatalog);

  await Promise.all([loadHoldings(), loadHistory(), loadCatalog()]);
}

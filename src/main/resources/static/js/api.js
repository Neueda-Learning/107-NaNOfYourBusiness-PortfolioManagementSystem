/**
 * api.js — single place that knows the backend base URL.
 * All fetch calls go through the helpers exported from here.
 * Never call fetch() directly from other modules.
 */

const BASE_URL = "/api/v1";

/**
 * Shared fetch wrapper.
 * Throws an Error with a user-friendly message on non-2xx responses.
 */
async function apiFetch(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    let errorCode = null;
    try {
      const body = await res.json();
      message = body.message || message;
      errorCode = body.error || null;
    } catch (_) { /* ignore parse errors */ }
    const err = new Error(message);
    err.status = res.status;
    err.errorCode = errorCode;
    throw err;
  }

  // 204 No Content
  if (res.status === 204) return null;
  return res.json();
}

// ── Portfolio Summary ────────────────────────────────
/** @returns {Promise<import("./dashboard.js").SummaryData>} */
export async function getPortfolioSummary() {
  return apiFetch("/portfolio/summary");
}

// ── Portfolio Items ──────────────────────────────────
/**
 * @param {string|null} type  One of "STOCK" | "BOND" | "MUTUAL_FUND" | null (all)
 */
export async function getPortfolioItems(type = null) {
  const qs = type ? `?type=${encodeURIComponent(type)}` : "";
  return apiFetch(`/portfolio-items${qs}`);
}

export async function getPortfolioItem(id) {
  return apiFetch(`/portfolio-items/${id}`);
}

export async function createPortfolioItem(payload) {
  return apiFetch("/portfolio-items", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updatePortfolioItem(id, payload) {
  return apiFetch(`/portfolio-items/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deletePortfolioItem(id) {
  return apiFetch(`/portfolio-items/${id}`, { method: "DELETE" });
}

export async function buyPortfolioItem(id, quantity) {
  return apiFetch(`/portfolio-items/${id}/buy`, {
    method: "POST",
    body: JSON.stringify({ quantity }),
  });
}

export async function sellPortfolioItem(id, quantity) {
  return apiFetch(`/portfolio-items/${id}/sell`, {
    method: "POST",
    body: JSON.stringify({ quantity }),
  });
}

// ── Market Data ───────────────────────────────────────
export async function getSupportedTickers() {
  return apiFetch("/market/supported-tickers");
}

export async function getStockCatalog() {
  return apiFetch("/market/stock-catalog");
}

export async function getStockQuote(ticker) {
  const qs = `?ticker=${encodeURIComponent(ticker)}`;
  return apiFetch(`/market/quote${qs}`);
}

/**
 * Fetch cached quotes for multiple tickers in one call.
 * Uses the in-memory cache populated by the backend's scheduled poll (every 10 s).
 * Returns a map of { "TICKER": { ticker, price, currency, asOf }, ... }
 */
export async function getBatchQuotes(tickers) {
  if (!tickers || tickers.length === 0) return {};
  const qs = tickers.map(t => `tickers=${encodeURIComponent(t)}`).join("&");
  return apiFetch(`/market/batch-quotes?${qs}`);
}

// ── Mutual Funds ─────────────────────────────────────
/** GET /api/mutual-funds — all 30 supported funds with latest NAV */
export async function getMutualFunds() {
  const res = await fetch("/api/mutual-funds", { headers: { "Content-Type": "application/json" } });
  if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || `Request failed (${res.status})`); }
  return res.json();
}

/** GET /api/mutual-funds/{schemeCode} — raw MFAPI details */
export async function getMutualFundDetails(schemeCode) {
  const res = await fetch(`/api/mutual-funds/${schemeCode}`, { headers: { "Content-Type": "application/json" } });
  if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || `Request failed (${res.status})`); }
  return res.json();
}

/** POST /api/mutual-funds/buy */
export async function buyMutualFund(payload) {
  const res = await fetch("/api/mutual-funds/buy", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || `Request failed (${res.status})`); }
  return res.json();
}

/** POST /api/mutual-funds/sell */
export async function sellMutualFund(payload) {
  const res = await fetch("/api/mutual-funds/sell", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || `Request failed (${res.status})`); }
  return res.json();
}

// ── Portfolio Item Actions ────────────────────────────
/**
 * Force-refresh the stored currentPrice for a portfolio item from Yahoo Finance.
 * POST /api/v1/portfolio-items/{id}/refresh-price
 */
export async function refreshPortfolioItemPrice(id) {
  return apiFetch(`/portfolio-items/${id}/refresh-price`, { method: "POST" });
}

// ── Bonds ─────────────────────────────────────────────
/** GET /api/v1/bonds — active bond holdings */
export async function getBonds() {
  return apiFetch("/bonds");
}

/** GET /api/v1/bonds/redeemed — redemption history */
export async function getRedeemedBonds() {
  return apiFetch("/bonds/redeemed");
}

/** GET /api/v1/bonds/all — every bond in the database (ACTIVE + REDEEMED), for the catalog */
export async function getBondCatalog() {
  return apiFetch("/bonds/all");
}

/** POST /api/v1/bonds/buy */
export async function buyBond(payload) {
  return apiFetch("/bonds/buy", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/** POST /api/v1/bonds/redeem */
export async function redeemBond(symbol) {
  return apiFetch("/bonds/redeem", {
    method: "POST",
    body: JSON.stringify({ symbol }),
  });
}



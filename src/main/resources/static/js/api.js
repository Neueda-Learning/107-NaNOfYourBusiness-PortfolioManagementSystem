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
    try {
      const body = await res.json();
      message = body.message || message;
    } catch (_) { /* ignore parse errors */ }
    throw new Error(message);
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


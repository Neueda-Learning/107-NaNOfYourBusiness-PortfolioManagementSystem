/**
 * marketBrowse.js — Stocks tab (US-03)
 *
 * Features:
 *  - Featured stocks browse list (24 symbols, alphabetical) with cached prices
 *  - Ticker autocomplete from stock catalog
 *  - Live quote lookup via backend cache
 *  - Add stock holdings
 *  - Per-row refresh price + buy/sell actions for existing holdings
 */

import {
  buyPortfolioItem,
  createPortfolioItem,
  getBatchQuotes,
  getPortfolioItems,
  getStockCatalog,
  getStockQuote,
  refreshPortfolioItemPrice,
  sellPortfolioItem,
} from "./api.js";

let initialized = false;
let lastQuote = null;
let stockCatalog = [];
let featuredTickers = [];
let filteredTickers = [];
let activeSuggestion = -1;
let heldItems = [];
let liveTimer = null;
let featuredCollapsed = true;
const LIVE_INTERVAL = 10_000;
const FEATURED_COUNT = 24;

const byId = (id) => document.getElementById(id);

function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function fmtNum(val, decimals = 2) {
  if (val == null || isNaN(val)) return "—";
  return Number(val).toLocaleString("en-IN", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

async function loadTickers() {
  try {
    const catalog = await getStockCatalog();
    stockCatalog = Array.isArray(catalog) ? catalog : [];
  } catch (err) {
    stockCatalog = [];
    console.warn("Could not load stock catalog:", err.message);
  }
}

function pickFeaturedTickersFromCatalog() {
  featuredTickers = stockCatalog
    .map((item) => item.symbol)
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b))
    .slice(0, FEATURED_COUNT);
}

function updateFeaturedStatus() {
  const el = byId("browse-featured-status");
  if (el) el.textContent = `Updated ${new Date().toLocaleTimeString()}`;
}

function toggleFeaturedCollapse() {
  featuredCollapsed = !featuredCollapsed;
  const wrap = byId("featured-stocks-collapsible");
  const btn = byId("browse-toggle-featured-btn");
  if (wrap) wrap.classList.toggle("is-collapsed", featuredCollapsed);
  if (btn) btn.setAttribute("aria-expanded", String(!featuredCollapsed));
}

function renderFeaturedRows(quotes = {}) {
  const body = byId("browse-featured-body");
  if (!body) return;

  if (featuredTickers.length === 0) {
    body.innerHTML = '<p style="color:var(--color-text-faint);font-size:var(--font-size-sm);padding:var(--space-2) 0;">No featured stocks configured.</p>';
    return;
  }

  body.innerHTML = "";
  featuredTickers.forEach((ticker) => {
    const quote = quotes[ticker];
    const price = quote?.price != null ? fmtNum(quote.price) : null;

    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "featured-ticker-chip js-featured-select";
    chip.dataset.ticker = ticker;
    chip.dataset.featuredTicker = ticker;
    chip.innerHTML = `
      <span class="featured-chip__symbol">${ticker}</span>
      <span class="featured-chip__price js-featured-price${price ? "" : " featured-chip__price--empty"}">${price ?? "—"}</span>
    `;
    chip.addEventListener("click", async () => {
      const tickerInput = byId("browse-ticker");
      if (tickerInput) tickerInput.value = ticker;
      await handleFetchQuote();
    });
    body.appendChild(chip);
  });
}

async function loadFeaturedStocks() {
  const body = byId("browse-featured-body");
  if (!body) return;
  body.innerHTML = '<p style="color:var(--color-text-faint);font-size:var(--font-size-sm);padding:var(--space-2) 0;">Loading…</p>';

  if (featuredTickers.length === 0) {
    renderFeaturedRows({});
    return;
  }

  try {
    const quotes = await getBatchQuotes(featuredTickers);
    renderFeaturedRows(quotes);
    updateFeaturedStatus();
  } catch (err) {
    const p = document.createElement("p");
    p.style.cssText = "color:var(--color-error-text);font-size:var(--font-size-sm);padding:var(--space-2) 0;";
    p.textContent = `Could not load prices: ${err.message}`;
    body.innerHTML = "";
    body.appendChild(p);
  }
}

function closeDropdown() {
  const d = byId("browse-ticker-dropdown");
  if (!d) return;
  d.style.display = "none";
  d.innerHTML = "";
  activeSuggestion = -1;
}

function selectTicker(item) {
  const input = byId("browse-ticker");
  if (input) input.value = item.symbol;
  lastQuote = null;
  closeDropdown();
}

function renderDropdown() {
  const d = byId("browse-ticker-dropdown");
  if (!d) return;
  d.innerHTML = "";

  if (filteredTickers.length === 0) {
    const empty = document.createElement("div");
    empty.className = "stock-autocomplete__empty";
    empty.textContent = "No matching tickers";
    d.appendChild(empty);
    d.style.display = "block";
    return;
  }

  filteredTickers.forEach((item, i) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "stock-autocomplete__item" + (i === activeSuggestion ? " is-active" : "");
    btn.innerHTML = `
      <div class="stock-autocomplete__item-symbol">${item.symbol}</div>
      <div class="stock-autocomplete__item-name">${item.companyName} · ${item.currency}</div>
    `;
    btn.addEventListener("mousedown", (e) => {
      e.preventDefault();
      selectTicker(item);
    });
    d.appendChild(btn);
  });
  d.style.display = "block";
}

function updateSuggestions() {
  const query = (byId("browse-ticker")?.value || "").trim().toUpperCase();
  if (!query) {
    closeDropdown();
    return;
  }
  filteredTickers = stockCatalog
    .filter((item) =>
      item.symbol.toUpperCase().startsWith(query) ||
      (item.companyName || "").toUpperCase().startsWith(query)
    )
    .slice(0, 20);
  activeSuggestion = filteredTickers.length > 0 ? 0 : -1;
  renderDropdown();
}

function handleTickerKeydown(e) {
  if (filteredTickers.length === 0) {
    if (e.key === "Escape") closeDropdown();
    return;
  }
  if (e.key === "ArrowDown") {
    e.preventDefault();
    activeSuggestion = (activeSuggestion + 1) % filteredTickers.length;
    renderDropdown();
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    activeSuggestion = (activeSuggestion - 1 + filteredTickers.length) % filteredTickers.length;
    renderDropdown();
  } else if (e.key === "Enter") {
    if (activeSuggestion >= 0) {
      e.preventDefault();
      selectTicker(filteredTickers[activeSuggestion]);
    }
  } else if (e.key === "Escape") {
    closeDropdown();
  }
}

function wireOutsideClick() {
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".stock-autocomplete")) closeDropdown();
  });
}

function getNormalizedTicker() {
  return (byId("browse-ticker")?.value || "").trim().toUpperCase();
}

function resolveCatalogItem(query) {
  if (!query) return null;
  const up = query.toUpperCase();
  const exact = stockCatalog.find((i) => i.symbol.toUpperCase() === up
    || (i.companyName || "").toUpperCase() === up);
  if (exact) return exact;
  const prefix = stockCatalog.filter((i) => i.symbol.toUpperCase().startsWith(up)
    || (i.companyName || "").toUpperCase().startsWith(up));
  return prefix.length === 1 ? prefix[0] : null;
}

async function handleFetchQuote() {
  const query = getNormalizedTicker();
  const selected = resolveCatalogItem(query);
  const ticker = selected?.symbol ?? query;
  const quoteEl = byId("browse-quote");

  if (!ticker) {
    if (quoteEl) {
      quoteEl.textContent = "Please enter a ticker symbol.";
      quoteEl.className = "quote-result quote-result--error";
    }
    return;
  }

  const tickerInput = byId("browse-ticker");
  if (tickerInput) tickerInput.value = ticker;
  if (quoteEl) {
    quoteEl.textContent = `Fetching quote for ${ticker}…`;
    quoteEl.className = "quote-result";
  }

  try {
    const quote = await getStockQuote(ticker);
    lastQuote = quote;
    const asOf = quote.asOf ? new Date(quote.asOf).toLocaleTimeString() : "—";
    if (quoteEl) {
      quoteEl.innerHTML = `
        <span class="quote-result__ticker">${quote.ticker}</span>
        <span class="quote-result__price">${quote.currency} ${fmtNum(quote.price)}</span>
        <span class="quote-result__meta">as of ${asOf} · prices may be delayed ~15 min</span>
      `;
      quoteEl.className = "quote-result quote-result--price";
    }
  } catch (err) {
    lastQuote = null;
    if (quoteEl) {
      quoteEl.textContent = `Quote unavailable: ${err.message}`;
      quoteEl.className = "quote-result quote-result--error";
    }
  }
}

function setActionMsg(text, isError = false) {
  const el = byId("browse-add-result");
  if (!el) return;
  el.textContent = text;
  el.style.color = isError ? cssVar("--color-error-text") : cssVar("--color-gain");
}

function setHoldingsMsg(text, isError = false) {
  const el = byId("browse-holdings-result");
  if (!el) return;
  el.textContent = text;
  el.style.color = text
    ? (isError ? cssVar("--color-error-text") : cssVar("--color-gain"))
    : cssVar("--color-text-faint");
}

function buildHoldingsRow(item, livePrices) {
  const symbol = item.symbolOrName;
  const qty = Number(item.quantity ?? 0);
  const buyPrice = Number(item.purchasePrice ?? 0);
  const storedPrice = item.currentPrice != null ? Number(item.currentPrice) : null;
  const liveQuote = livePrices?.[symbol];
  const currentPrice = liveQuote ? Number(liveQuote.price) : storedPrice;

  let gainText = "—";
  let gainCls = "";
  if (currentPrice != null && qty > 0 && buyPrice > 0) {
    const gain = (currentPrice - buyPrice) * qty;
    const gainPct = ((currentPrice - buyPrice) / buyPrice) * 100;
    const sign = gain >= 0 ? "+" : "";
    gainCls = gain >= 0 ? "holdings-table__gain--positive" : "holdings-table__gain--negative";
    gainText = `${sign}${fmtNum(gain)} (${sign}${gainPct.toFixed(2)}%)`;
  }

  const priceDisplay = currentPrice != null ? fmtNum(currentPrice) : "—";
  const priceTitle = liveQuote ? "Source: live cache (~10 s)" : "Source: stored";

  const tr = document.createElement("tr");
  tr.dataset.ticker = symbol;
  tr.dataset.itemId = item.id;
  tr.innerHTML = `
    <td class="holdings-table__symbol">${symbol}</td>
    <td class="holdings-table__price">${fmtNum(qty, 4).replace(/\.?0+$/, "")}</td>
    <td class="holdings-table__price">${fmtNum(buyPrice)}</td>
    <td class="holdings-table__price js-current-price" title="${priceTitle}">${priceDisplay}</td>
    <td class="holdings-table__gain ${gainCls} js-gain-cell">${gainText}</td>
    <td class="holdings-table__actions">
      <button type="button" class="btn-refresh-price js-refresh-btn"
              data-id="${item.id}" title="Refresh price from market">↻</button>
      <div class="holdings-trade">
        <input type="number" min="0.0001" step="0.0001" value="1"
               class="input-field holdings-trade__qty js-trade-qty"
               aria-label="Trade quantity for ${symbol}">
        <button type="button" class="btn-secondary btn-trade-buy js-buy-btn" data-id="${item.id}">Buy</button>
        <button type="button" class="btn-secondary btn-trade-sell js-sell-btn" data-id="${item.id}">Sell</button>
      </div>
    </td>
  `;
  return tr;
}

function updateRowGainLoss(row, item) {
  const gainCell = row.querySelector(".js-gain-cell");
  if (!gainCell) return;

  const qty = Number(item.quantity ?? 0);
  const buyPrice = Number(item.purchasePrice ?? 0);
  const currentPrice = item.currentPrice != null ? Number(item.currentPrice) : null;
  if (currentPrice == null || qty <= 0 || buyPrice <= 0) {
    gainCell.textContent = "—";
    gainCell.className = "holdings-table__gain js-gain-cell";
    return;
  }

  const gain = (currentPrice - buyPrice) * qty;
  const gainPct = ((currentPrice - buyPrice) / buyPrice) * 100;
  const sign = gain >= 0 ? "+" : "";
  const cls = gain >= 0 ? "holdings-table__gain--positive" : "holdings-table__gain--negative";
  gainCell.textContent = `${sign}${fmtNum(gain)} (${sign}${gainPct.toFixed(2)}%)`;
  gainCell.className = `holdings-table__gain ${cls} js-gain-cell`;
}

function wireRowActions(row) {
  const refreshBtn = row.querySelector(".js-refresh-btn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", async () => {
      const id = refreshBtn.dataset.id;
      refreshBtn.disabled = true;
      refreshBtn.textContent = "…";
      try {
        const updated = await refreshPortfolioItemPrice(id);
        const priceCell = row.querySelector(".js-current-price");
        if (priceCell && updated.currentPrice != null) {
          priceCell.textContent = fmtNum(updated.currentPrice);
          priceCell.title = "Source: refreshed from market";
        }
        updateRowGainLoss(row, updated);
        const idx = heldItems.findIndex((i) => String(i.id) === String(id));
        if (idx >= 0) heldItems[idx] = { ...heldItems[idx], currentPrice: updated.currentPrice };
        setHoldingsMsg(`Refreshed ${updated.symbolOrName || row.dataset.ticker} price.`);
        refreshBtn.textContent = "✓";
        setTimeout(() => {
          refreshBtn.disabled = false;
          refreshBtn.textContent = "↻";
        }, 2000);
        if (typeof window.__markDashboardStale === "function") window.__markDashboardStale();
      } catch (err) {
        refreshBtn.textContent = "✗";
        setTimeout(() => {
          refreshBtn.disabled = false;
          refreshBtn.textContent = "↻";
        }, 2000);
        setHoldingsMsg(`Refresh failed: ${err.message}`, true);
      }
    });
  }

  const buyBtn = row.querySelector(".js-buy-btn");
  const sellBtn = row.querySelector(".js-sell-btn");
  const qtyInput = row.querySelector(".js-trade-qty");

  function readTradeQuantity() {
    const quantity = Number(qtyInput?.value);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      throw new Error("Trade quantity must be greater than 0.");
    }
    return quantity;
  }

  async function executeTrade(side) {
    const id = (side === "buy" ? buyBtn : sellBtn)?.dataset.id;
    if (!id) return;

    let quantity;
    try {
      quantity = readTradeQuantity();
    } catch (err) {
      setHoldingsMsg(err.message, true);
      return;
    }

    if (buyBtn) buyBtn.disabled = true;
    if (sellBtn) sellBtn.disabled = true;

    try {
      const updated = side === "buy"
        ? await buyPortfolioItem(id, quantity)
        : await sellPortfolioItem(id, quantity);
      const verb = side === "buy" ? "Bought" : "Sold";
      stopLiveTimer();
      await loadPortfolioStocks();
      setHoldingsMsg(`${verb} ${fmtNum(quantity, 4).replace(/\.?0+$/, "")} ${updated.symbolOrName} at market price.`);
      if (typeof window.__markDashboardStale === "function") window.__markDashboardStale();
    } catch (err) {
      setHoldingsMsg(`${side === "buy" ? "Buy" : "Sell"} failed: ${err.message}`, true);
    } finally {
      if (buyBtn) buyBtn.disabled = false;
      if (sellBtn) sellBtn.disabled = false;
    }
  }

  if (buyBtn) buyBtn.addEventListener("click", async () => executeTrade("buy"));
  if (sellBtn) sellBtn.addEventListener("click", async () => executeTrade("sell"));
}

async function loadPortfolioStocks(livePrices) {
  const body = byId("browse-holdings-body");
  if (!body) return false;

  if (!livePrices) {
    body.innerHTML = '<tr><td colspan="6" class="holdings-table__empty">Loading…</td></tr>';
  }

  try {
    const items = await getPortfolioItems("STOCK");
    heldItems = Array.isArray(items) ? items : [];

    if (heldItems.length === 0) {
      body.innerHTML = '<tr><td colspan="6" class="holdings-table__empty">No stocks in portfolio yet — add one using the form.</td></tr>';
      setHoldingsMsg("");
      startLiveTimer();
      return true;
    }

    if (!livePrices) {
      const tickers = [...new Set(heldItems.map((i) => i.symbolOrName))];
      try {
        livePrices = await getBatchQuotes(tickers);
      } catch (_) {
        livePrices = {};
      }
    }

    body.innerHTML = "";
    heldItems.forEach((item) => {
      const row = buildHoldingsRow(item, livePrices);
      wireRowActions(row);
      body.appendChild(row);
    });

    setHoldingsMsg("");
    updatePricesStatus();
    startLiveTimer();
    return true;
  } catch (err) {
    body.innerHTML = `<tr><td colspan="6" class="holdings-table__empty" style="color:var(--color-error-text);">Could not load holdings: ${err.message}</td></tr>`;
    setHoldingsMsg(`Refresh failed: ${err.message}`, true);
    return false;
  }
}

function startLiveTimer() {
  if (liveTimer) return;
  liveTimer = setInterval(async () => {
    if (heldItems.length === 0 && featuredTickers.length === 0) return;
    const tickers = [...new Set([...heldItems.map((i) => i.symbolOrName), ...featuredTickers])];

    try {
      const quotes = await getBatchQuotes(tickers);

      const holdingsBody = byId("browse-holdings-body");
      if (holdingsBody) {
        Array.from(holdingsBody.querySelectorAll("tr[data-ticker]")).forEach((row) => {
          const ticker = row.dataset.ticker;
          const q = quotes[ticker];
          if (!q) return;

          const priceCell = row.querySelector(".js-current-price");
          if (priceCell) {
            priceCell.textContent = fmtNum(q.price);
            priceCell.title = "Source: live cache (~10 s)";
          }

          const item = heldItems.find((i) => i.symbolOrName === ticker);
          if (item) updateRowGainLoss(row, { ...item, currentPrice: q.price });
        });
        updatePricesStatus();
      }

      const featuredBody = byId("browse-featured-body");
      if (featuredBody) {
        Array.from(featuredBody.querySelectorAll("[data-featured-ticker]")).forEach((row) => {
          const ticker = row.dataset.featuredTicker;
          const q = quotes[ticker];
          if (!q) return;

          const priceCell = row.querySelector(".js-featured-price");
          if (priceCell) priceCell.textContent = fmtNum(q.price);
        });
        updateFeaturedStatus();
      }
    } catch (_) {
      // Backend logs quote refresh errors; UI keeps prior values.
    }
  }, LIVE_INTERVAL);
}

function stopLiveTimer() {
  if (liveTimer) {
    clearInterval(liveTimer);
    liveTimer = null;
  }
}

function updatePricesStatus() {
  const el = byId("browse-prices-status");
  if (el) el.textContent = `Updated ${new Date().toLocaleTimeString()}`;
}

async function handleAddStock() {
  const query = getNormalizedTicker();
  const selected = resolveCatalogItem(query);
  const ticker = selected?.symbol ?? query;
  const qty = Number(byId("browse-qty")?.value);
  const purchaseDate = byId("browse-date")?.value;

  if (!ticker) {
    setActionMsg("Enter a ticker first.", true);
    return;
  }
  const tickerInput = byId("browse-ticker");
  if (tickerInput) tickerInput.value = ticker;
  if (!purchaseDate) {
    setActionMsg("Choose a purchase date.", true);
    return;
  }
  if (!Number.isFinite(qty) || qty <= 0) {
    setActionMsg("Quantity must be greater than 0.", true);
    return;
  }

  // purchasePrice is intentionally omitted for STOCK items.
  // The backend will auto-fetch the current market price and record it
  // as the purchase price (see StockPortfolioItemTypeHandler).
  const payload = {
    type: "STOCK",
    symbolOrName: ticker,
    quantity: qty,
    purchaseDate,
  };

  setActionMsg("Adding stock at current market price…");
  const addBtn = byId("browse-add-btn");
  if (addBtn) addBtn.disabled = true;

  try {
    const created = await createPortfolioItem(payload);
    const price = created.purchasePrice != null ? ` at ${fmtNum(created.purchasePrice)}` : "";
    setActionMsg(`Added ${created.symbolOrName}${price} (id: ${created.id}).`);
    const qtyEl = byId("browse-qty");
    if (qtyEl) qtyEl.value = "";
    lastQuote = null;
    stopLiveTimer();
    await loadPortfolioStocks();
    if (typeof window.__markDashboardStale === "function") window.__markDashboardStale();
  } catch (err) {
    setActionMsg(`Add failed: ${err.message}`, true);
  } finally {
    if (addBtn) addBtn.disabled = false;
  }
}

export async function loadMarketBrowse() {
  if (!initialized) {
    initialized = true;

    const dateInput = byId("browse-date");
    if (dateInput && !dateInput.value) dateInput.value = todayISO();

    byId("browse-fetch-btn")?.addEventListener("click", handleFetchQuote);
    byId("browse-add-btn")?.addEventListener("click", handleAddStock);
    byId("browse-refresh-holdings-btn")?.addEventListener("click", async () => {
      stopLiveTimer();
      const ok = await loadPortfolioStocks();
      if (ok) setHoldingsMsg("Holdings refreshed.");
    });
    byId("browse-refresh-featured-btn")?.addEventListener("click", loadFeaturedStocks);
    byId("browse-toggle-featured-btn")?.addEventListener("click", toggleFeaturedCollapse);
    byId("browse-ticker")?.addEventListener("input", updateSuggestions);
    byId("browse-ticker")?.addEventListener("keydown", handleTickerKeydown);
    byId("browse-ticker")?.addEventListener("blur", () => setTimeout(closeDropdown, 120));
    wireOutsideClick();
  }

  await loadTickers();
  pickFeaturedTickersFromCatalog();
  await loadFeaturedStocks();
  await loadPortfolioStocks();
}

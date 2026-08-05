import {
  createPortfolioItem,
  deletePortfolioItem,
  getPortfolioItems,
  getStockCatalog,
  getStockQuote,
} from "./api.js";

let initialized = false;
let lastQuote = null;
let stockCatalog = [];
let filteredTickers = [];
let activeSuggestionIndex = -1;

function byId(id) {
  return document.getElementById(id);
}

function setText(id, text, color = "#0f172a") {
  const el = byId(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = color;
}

function getTodayISODate() {
  return new Date().toISOString().slice(0, 10);
}

async function loadTickers() {
  const tickerInput = byId("browse-ticker");
  if (!tickerInput) return;

  setText("browse-quote", "Loading stock catalog...");
  try {
    const catalog = await getStockCatalog();
    stockCatalog = Array.isArray(catalog) ? catalog : [];
    if (!tickerInput.value && stockCatalog.length > 0) {
      tickerInput.value = stockCatalog[0].symbol;
    }
    setText("browse-quote", "Stock catalog loaded. Start typing or click 'Get Quote'.");
  } catch (err) {
    stockCatalog = [];
    setText("browse-quote", `Could not load stock catalog: ${err.message}`, "#b91c1c");
  }
}

function getDropdownEl() {
  return byId("browse-ticker-dropdown");
}

function closeTickerDropdown() {
  const dropdown = getDropdownEl();
  if (!dropdown) return;
  dropdown.style.display = "none";
  dropdown.innerHTML = "";
  activeSuggestionIndex = -1;
}

function selectTicker(item) {
  const input = byId("browse-ticker");
  if (!input) return;
  input.value = item.symbol;
  lastQuote = null;
  closeTickerDropdown();
}

function renderTickerDropdown() {
  const dropdown = getDropdownEl();
  if (!dropdown) return;

  dropdown.innerHTML = "";
  if (filteredTickers.length === 0) {
    const empty = document.createElement("div");
    empty.className = "stock-autocomplete__empty";
    empty.textContent = "No matching tickers";
    dropdown.appendChild(empty);
    dropdown.style.display = "block";
    return;
  }

  filteredTickers.forEach((itemData, index) => {
    const item = document.createElement("button");
    item.type = "button";
    item.className = "stock-autocomplete__item";
    if (index === activeSuggestionIndex) {
      item.classList.add("is-active");
    }
    item.innerHTML = `
      <div style="font-weight:600;color:#0f172a;">${itemData.symbol}</div>
      <div style="font-size:0.8rem;color:#64748b;">${itemData.companyName} · ${itemData.currency}</div>
    `;
    // mousedown prevents input blur from closing list before click selects.
    item.addEventListener("mousedown", (evt) => {
      evt.preventDefault();
      selectTicker(itemData);
    });
    dropdown.appendChild(item);
  });
  dropdown.style.display = "block";
}

function updateTickerSuggestions() {
  const raw = byId("browse-ticker")?.value || "";
  const query = raw.trim().toUpperCase();

  filteredTickers = !query
    ? stockCatalog.slice(0, 20)
    : stockCatalog.filter((item) => {
        const symbol = item.symbol.toUpperCase();
        const companyName = (item.companyName || "").toUpperCase();
        return symbol.startsWith(query) || companyName.startsWith(query);
      }).slice(0, 20);

  activeSuggestionIndex = filteredTickers.length > 0 ? 0 : -1;
  renderTickerDropdown();
}

function handleTickerKeydown(evt) {
  if (filteredTickers.length === 0) {
    if (evt.key === "Escape") closeTickerDropdown();
    return;
  }

  if (evt.key === "ArrowDown") {
    evt.preventDefault();
    activeSuggestionIndex = (activeSuggestionIndex + 1) % filteredTickers.length;
    renderTickerDropdown();
    return;
  }

  if (evt.key === "ArrowUp") {
    evt.preventDefault();
    activeSuggestionIndex = (activeSuggestionIndex - 1 + filteredTickers.length) % filteredTickers.length;
    renderTickerDropdown();
    return;
  }

  if (evt.key === "Enter") {
    if (activeSuggestionIndex >= 0 && filteredTickers[activeSuggestionIndex]) {
      evt.preventDefault();
      selectTicker(filteredTickers[activeSuggestionIndex]);
    }
    return;
  }

  if (evt.key === "Escape") {
    closeTickerDropdown();
  }
}

function wireOutsideClickClose() {
  document.addEventListener("click", (evt) => {
    const wrapper = evt.target.closest(".stock-autocomplete");
    if (!wrapper) {
      closeTickerDropdown();
    }
  });
}

function getNormalizedTicker() {
  const raw = byId("browse-ticker")?.value || "";
  return raw.trim().toUpperCase();
}

function resolveCatalogSelection(query) {
  if (!query) return null;

  const exact = stockCatalog.find((item) =>
    item.symbol.toUpperCase() === query || (item.companyName || "").toUpperCase() === query
  );
  if (exact) return exact;

  const prefixMatches = stockCatalog.filter((item) => {
    const symbol = item.symbol.toUpperCase();
    const companyName = (item.companyName || "").toUpperCase();
    return symbol.startsWith(query) || companyName.startsWith(query);
  });

  return prefixMatches.length === 1 ? prefixMatches[0] : null;
}

async function handleFetchQuote() {
  const rawQuery = getNormalizedTicker();
  const selected = resolveCatalogSelection(rawQuery);
  const ticker = selected?.symbol ?? rawQuery;
  if (!ticker) {
    setText("browse-quote", "Please enter a ticker symbol.", "#b91c1c");
    return;
  }

  byId("browse-ticker").value = ticker;

  setText("browse-quote", `Fetching live quote for ${ticker}...`);
  try {
    const quote = await getStockQuote(ticker);
    lastQuote = quote;
    setText(
      "browse-quote",
      `${quote.ticker}: ${quote.currency} ${Number(quote.price).toFixed(2)}`
    );
  } catch (err) {
    lastQuote = null;
    setText("browse-quote", `Quote error: ${err.message}`, "#b91c1c");
  }
}

async function loadPortfolioStocks() {
  const body = byId("browse-holdings-body");
  if (!body) return;

  body.innerHTML = '<tr><td colspan="5" style="padding:10px;color:#64748b;">Loading...</td></tr>';
  try {
    const items = await getPortfolioItems("STOCK");
    if (!items || items.length === 0) {
      body.innerHTML = '<tr><td colspan="5" style="padding:10px;color:#64748b;">No stocks in portfolio yet.</td></tr>';
      return;
    }

    body.innerHTML = "";
    items.forEach((item) => {
      const tr = document.createElement("tr");
      tr.style.borderBottom = "1px solid #e2e8f0";
      tr.innerHTML = `
        <td style="padding:8px;">${item.symbolOrName}</td>
        <td style="padding:8px;">${item.quantity ?? "-"}</td>
        <td style="padding:8px;">${item.purchasePrice ?? "-"}</td>
        <td style="padding:8px;">${item.currentPrice ?? "-"}</td>
        <td style="padding:8px;"><button type="button" class="btn-remove" data-remove-id="${item.id}">Remove</button></td>
      `;
      body.appendChild(tr);
    });

    body.querySelectorAll("button[data-remove-id]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-remove-id");
        if (!id) return;
        btn.disabled = true;
        try {
          await deletePortfolioItem(id);
          setText("browse-add-result", "Stock removed from portfolio.", "#166534");
          await loadPortfolioStocks();
          if (typeof window.__markDashboardStale === "function") {
            window.__markDashboardStale();
          }
        } catch (err) {
          btn.disabled = false;
          setText("browse-add-result", `Remove failed: ${err.message}`, "#b91c1c");
        }
      });
    });
  } catch (err) {
    body.innerHTML = `<tr><td colspan="5" style="padding:10px;color:#b91c1c;">Could not load holdings: ${err.message}</td></tr>`;
  }
}

async function handleAddStock() {
  const rawQuery = getNormalizedTicker();
  const selected = resolveCatalogSelection(rawQuery);
  const ticker = selected?.symbol ?? rawQuery;
  const quantity = Number(byId("browse-qty")?.value);
  const purchasePrice = Number(byId("browse-purchase-price")?.value);
  const purchaseDate = byId("browse-date")?.value;

  if (!ticker) {
    setText("browse-add-result", "Enter a ticker first.", "#b91c1c");
    return;
  }
  byId("browse-ticker").value = ticker;
  if (!purchaseDate) {
    setText("browse-add-result", "Choose purchase date.", "#b91c1c");
    return;
  }
  if (!Number.isFinite(quantity) || quantity <= 0) {
    setText("browse-add-result", "Quantity must be greater than 0.", "#b91c1c");
    return;
  }
  if (!Number.isFinite(purchasePrice) || purchasePrice <= 0) {
    setText("browse-add-result", "Purchase price must be greater than 0.", "#b91c1c");
    return;
  }

  const payload = {
    type: "STOCK",
    symbolOrName: ticker,
    quantity,
    purchasePrice,
    purchaseDate,
    currentPrice: lastQuote?.price ?? null,
  };

  setText("browse-add-result", "Adding stock...");
  try {
    const created = await createPortfolioItem(payload);
    setText("browse-add-result", `Added ${created.symbolOrName} (id: ${created.id}).`, "#166534");
    await loadPortfolioStocks();

    if (typeof window.__markDashboardStale === "function") {
      window.__markDashboardStale();
    }
  } catch (err) {
    setText("browse-add-result", `Add failed: ${err.message}`, "#b91c1c");
  }
}

export async function loadMarketBrowse() {
  if (!initialized) {
    initialized = true;

    const dateInput = byId("browse-date");
    if (dateInput && !dateInput.value) {
      dateInput.value = getTodayISODate();
    }

    byId("browse-fetch-btn")?.addEventListener("click", handleFetchQuote);
    byId("browse-add-btn")?.addEventListener("click", handleAddStock);
    byId("browse-refresh-holdings-btn")?.addEventListener("click", loadPortfolioStocks);
    byId("browse-ticker")?.addEventListener("input", updateTickerSuggestions);
    byId("browse-ticker")?.addEventListener("focus", updateTickerSuggestions);
    byId("browse-ticker")?.addEventListener("keydown", handleTickerKeydown);
    byId("browse-ticker")?.addEventListener("blur", () => {
      setTimeout(closeTickerDropdown, 120);
    });
    wireOutsideClickClose();
  }

  await loadTickers();
  updateTickerSuggestions();
  await loadPortfolioStocks();
}


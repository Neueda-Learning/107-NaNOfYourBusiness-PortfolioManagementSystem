/**
 * walletTransactions.js — Wallet Transactions tab
 *
 * Displays the full wallet ledger (deposits, buy debits, sell credits)
 * sourced from GET /api/v1/wallet/transactions.
 */

import { getWalletTransactions } from "./api.js";

let initialized = false;

const byId = (id) => document.getElementById(id);

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

function fmtTimestamp(val) {
  if (!val) return "—";
  const d = new Date(val);
  if (isNaN(d.getTime())) return String(val);
  return d.toLocaleString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

function setMsg(id, text, isError = false) {
  const el = byId(id);
  if (!el) return;
  el.textContent = text;
  el.style.color = isError ? "var(--color-danger, #e53e3e)" : "var(--color-success, #38a169)";
}

const TYPE_LABELS = {
  DEPOSIT: "Deposit",
  BUY_DEBIT: "Buy (Debit)",
  SELL_CREDIT: "Sell (Credit)",
};

function typeBadge(type) {
  const label = TYPE_LABELS[type] || type || "—";
  const isCredit = type === "DEPOSIT" || type === "SELL_CREDIT";
  const color = isCredit ? "var(--color-success,#38a169)" : "var(--color-danger,#e53e3e)";
  const sign = isCredit ? "+" : "−";
  return { label, color, sign };
}

async function loadTransactions() {
  const tbody = byId("wallet-tx-body");
  if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">Loading…</td></tr>`;
  setMsg("wallet-tx-msg", "");

  try {
    const txs = await getWalletTransactions();
    renderTransactions(Array.isArray(txs) ? txs : []);
  } catch (err) {
    setMsg("wallet-tx-msg", "Failed to load transactions: " + err.message, true);
    if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">Error loading transactions.</td></tr>`;
  }
}

function renderTransactions(txs) {
  const tbody = byId("wallet-tx-body");
  if (!tbody) return;

  if (!txs.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="holdings-table__empty">No wallet transactions yet. Deposit funds to get started.</td></tr>`;
    return;
  }

  // Most recent first
  const sorted = [...txs].sort((a, b) => {
    const ta = new Date(a.createdAt || a.timestamp || 0).getTime();
    const tb = new Date(b.createdAt || b.timestamp || 0).getTime();
    return tb - ta;
  });

  tbody.innerHTML = sorted.map(tx => {
    const { label, color, sign } = typeBadge(tx.type || tx.transactionType);
    return `
      <tr>
        <td>${fmtTimestamp(tx.createdAt || tx.timestamp)}</td>
        <td><span style="color:${color};font-weight:600;">${label}</span></td>
        <td><span style="color:${color};">${sign}${fmtCurrency(tx.amount)}</span></td>
        <td>${fmtCurrency(tx.balanceAfter)}</td>
        <td>${tx.assetType || "—"}</td>
        <td>${tx.symbolOrName || "—"}</td>
      </tr>`;
  }).join("");
}

// ── Public entry point ────────────────────────────────

export async function loadWalletTransactions() {
  if (!initialized) {
    initialized = true;
    byId("wallet-tx-refresh-btn")?.addEventListener("click", loadTransactions);
  }
  await loadTransactions();
}

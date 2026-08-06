/**
 * wallet.js — Wallet tab
 *
 * Features:
 *  - View current wallet balance
 *  - Deposit funds into the wallet
 *
 * The wallet funds all buy transactions (stocks/bonds/mutual funds) and
 * receives credit whenever a holding is sold/redeemed. Full history lives
 * in the separate "Transactions" tab (see walletTransactions.js).
 */

import { getWalletBalance, depositToWallet } from "./api.js";

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

// ── Balance ───────────────────────────────────────────

async function loadBalance() {
  const valueEl = byId("wallet-balance-value");
  const updatedEl = byId("wallet-balance-updated");
  if (valueEl) valueEl.textContent = "…";

  try {
    const res = await getWalletBalance();
    if (valueEl) valueEl.textContent = fmtCurrency(res.balance);
    if (updatedEl) updatedEl.textContent = "As of " + fmtTimestamp(res.timestamp);
  } catch (err) {
    if (valueEl) valueEl.textContent = "—";
    if (updatedEl) updatedEl.textContent = "Failed to load balance: " + err.message;
  }
}

// ── Deposit ───────────────────────────────────────────

async function handleDeposit() {
  const input = byId("wallet-deposit-amount");
  const btn = byId("wallet-deposit-btn");
  const amount = parseFloat(input?.value);

  if (!amount || amount <= 0) {
    setMsg("wallet-deposit-result", "Enter a valid deposit amount greater than 0.", true);
    return;
  }

  if (btn) btn.disabled = true;
  setMsg("wallet-deposit-result", "");

  try {
    const res = await depositToWallet(amount);
    setMsg("wallet-deposit-result", `Deposited ${fmtCurrency(amount)}. New balance: ${fmtCurrency(res.balance)}.`);
    if (input) input.value = "";
    await loadBalance();
    window.__markDashboardStale?.();
  } catch (err) {
    setMsg("wallet-deposit-result", "Deposit failed: " + err.message, true);
  } finally {
    if (btn) btn.disabled = false;
  }
}

// ── Public entry point ────────────────────────────────

export async function loadWallet() {
  if (!initialized) {
    initialized = true;
    byId("wallet-refresh-btn")?.addEventListener("click", loadBalance);
    byId("wallet-deposit-btn")?.addEventListener("click", handleDeposit);
  }
  await loadBalance();
}

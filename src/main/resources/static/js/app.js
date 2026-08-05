/**
 * app.js — application entry point.
 *
 * Responsibilities:
 *  - Wire up tab navigation (show/hide panels)
 *  - On first load, activate the Dashboard tab and trigger its data fetch
 *  - Lazy-load other tabs (stubbed for now; portfolio tabs come in later stories)
 */

import { loadDashboard } from "./dashboard.js";
import { loadMarketBrowse } from "./marketBrowse.js";

// Track which tabs have been loaded to avoid redundant re-fetches within a session
const _loaded = new Set();

/** Activate a tab by its data-tab attribute value */
function activateTab(tabName) {
  // Update button states
  document.querySelectorAll(".tab-nav__btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === tabName);
  });

  // Show matching panel, hide others
  document.querySelectorAll(".tab-panel").forEach(panel => {
    panel.classList.toggle("active", panel.dataset.panel === tabName);
  });

  // Trigger data load for the tab (lazy — only once per session)
  if (!_loaded.has(tabName)) {
    _loaded.add(tabName);
    onTabFirstLoad(tabName);
  }
}

/** Called the first time a tab becomes active */
function onTabFirstLoad(tabName) {
  switch (tabName) {
    case "dashboard":
      loadDashboard();
      break;
    case "stocks":
      loadMarketBrowse();
      break;
    // Future tabs will add cases here (US-03/04/05)
    default:
      break;
  }
}

/** Allow the dashboard retry button to trigger a reload */
window.__reloadDashboard = () => {
  _loaded.delete("dashboard");
  activateTab("dashboard");
};

// Called by other modules after portfolio mutations so dashboard refreshes next time.
window.__markDashboardStale = () => {
  _loaded.delete("dashboard");
};

// ── Bootstrap ────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
  // Wire tab button clicks
  document.querySelectorAll(".tab-nav__btn").forEach(btn => {
    btn.addEventListener("click", () => activateTab(btn.dataset.tab));
  });

  // Open Dashboard on first load
  activateTab("dashboard");
});


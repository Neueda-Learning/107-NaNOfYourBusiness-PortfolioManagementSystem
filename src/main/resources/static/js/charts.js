/**
 * charts.js — Chart.js setup helpers.
 * Requires Chart.js to be loaded globally via CDN before this module is used.
 */

/** Read a CSS custom property from the document root */
function getCssVar(name, fallback = "") {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

/** Map asset types to theme-aware CSS custom property colors */
function getAssetColorMap() {
  return {
    STOCK:       getCssVar("--color-stock", "#7c3aed"),
    BOND:        getCssVar("--color-bond", "#d97706"),
    MUTUAL_FUND: getCssVar("--color-mutual-fund", "#059669"),
  };
}

/** Human-readable labels for each type */
const ASSET_LABELS = {
  STOCK:       "Stocks",
  BOND:        "Bonds",
  MUTUAL_FUND: "Mutual Funds",
};

/** Keep track of active Chart instances keyed by canvas ID so we can destroy before re-render */
const _chartInstances = {};

/**
 * Render (or re-render) the allocation doughnut chart.
 *
 * @param {string} canvasId  The id of the <canvas> element
 * @param {Array<{type:string, value:number, percent:number, count:number}>} allocationData
 */
export function renderAllocationChart(canvasId, allocationData) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;

  // Destroy previous instance to avoid Chart.js duplicate-canvas warnings
  if (_chartInstances[canvasId]) {
    _chartInstances[canvasId].destroy();
    delete _chartInstances[canvasId];
  }

  const assetColors = getAssetColorMap();
  const labels      = allocationData.map(d => ASSET_LABELS[d.type] ?? d.type);
  const data        = allocationData.map(d => parseFloat(d.value));
  const percents    = allocationData.map(d => parseFloat(d.percent));
  const counts      = allocationData.map(d => d.count);
  const colors      = allocationData.map(d => assetColors[d.type] ?? getCssVar("--color-text-faint", "#94a3b8"));

  const legendColor     = getCssVar("--color-text", "#111827");
  const tooltipBg       = getCssVar("--color-surface", "#ffffff");
  const tooltipText     = getCssVar("--color-text", "#111827");
  const tooltipBorder   = getCssVar("--color-border", "rgba(0,0,0,0.1)");
  const segmentBorder   = getCssVar("--color-bg", "#ffffff");

  // Apply theme-aware default text color for all chart plugin text.
  Chart.defaults.color = legendColor;

  _chartInstances[canvasId] = new Chart(canvas, {
    type: "doughnut",
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: colors,
        borderColor: segmentBorder,
        borderWidth: 3,
        hoverBorderWidth: 3,
      }],
    },
    options: {
      cutout: "62%",
      animation: { animateScale: true, duration: 500 },
      plugins: {
        legend: {
          position: "bottom",
          labels: {
            color: legendColor,
            padding: 16,
            font: { size: 13 },
            generateLabels(chart) {
              return chart.data.labels.map((label, i) => ({
                text: `${label}  ${percents[i]}%  (${counts[i]} holding${counts[i] !== 1 ? "s" : ""})`,
                fillStyle: colors[i],
                strokeStyle: colors[i],
                color: legendColor,
                fontColor: legendColor,
                hidden: false,
                index: i,
              }));
            },
          },
        },
        tooltip: {
          backgroundColor: tooltipBg,
          titleColor: tooltipText,
          bodyColor: tooltipText,
          borderColor: tooltipBorder,
          borderWidth: 1,
          callbacks: {
            label(ctx) {
              const value = ctx.parsed;
              const pct   = percents[ctx.dataIndex];
              const cnt   = counts[ctx.dataIndex];
              const fmt   = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
              return [
                ` ${fmt.format(value)}  (${pct}%)`,
                ` ${cnt} holding${cnt !== 1 ? "s" : ""}`,
              ];
            },
          },
        },
      },
    },
  });
}


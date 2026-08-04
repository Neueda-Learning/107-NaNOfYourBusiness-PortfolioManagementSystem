/**
 * charts.js — Chart.js setup helpers.
 * Requires Chart.js to be loaded globally via CDN before this module is used.
 */

/** Map asset type strings to CSS custom property colors */
const ASSET_COLORS = {
  STOCK:       "#3b82f6",
  BOND:        "#f59e0b",
  MUTUAL_FUND: "#10b981",
};

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

  const labels   = allocationData.map(d => ASSET_LABELS[d.type] ?? d.type);
  const data     = allocationData.map(d => parseFloat(d.value));
  const percents = allocationData.map(d => parseFloat(d.percent));
  const counts   = allocationData.map(d => d.count);
  const colors   = allocationData.map(d => ASSET_COLORS[d.type] ?? "#94a3b8");

  _chartInstances[canvasId] = new Chart(canvas, {
    type: "doughnut",
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: colors,
        borderColor: "#ffffff",
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
            padding: 16,
            font: { size: 13 },
            generateLabels(chart) {
              return chart.data.labels.map((label, i) => ({
                text: `${label}  ${percents[i]}%  (${counts[i]} holding${counts[i] !== 1 ? "s" : ""})`,
                fillStyle: colors[i],
                strokeStyle: colors[i],
                hidden: false,
                index: i,
              }));
            },
          },
        },
        tooltip: {
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


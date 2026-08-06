/**
 * charts.js — Chart.js setup helpers.
 * Requires Chart.js to be loaded globally via CDN before this module is used.
 */

/** Read a CSS custom property from the document root */
function getCssVar(name, fallback = "") {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

/** Is the app currently in dark mode? */
function isDarkTheme() {
  return document.documentElement.getAttribute("data-theme") === "dark";
}

/**
 * Resolve a text/surface design token directly from the active theme, bypassing
 * `getComputedStyle` for these specific values. Chart.js draws onto a <canvas>
 * with JS-supplied hex/rgba strings — if a CSS variable read ever returns stale
 * or empty (e.g. relative render-order timing), the canvas text silently keeps
 * whatever fallback was hard-coded, which previously defaulted to a *dark* hex
 * regardless of theme — invisible against a light card background. These
 * mirror the literal values in variables.css so they always match the theme.
 */
function themeTextColor(name) {
  const dark = isDarkTheme();
  const tokens = {
    text:      dark ? "#fafafa" : "#09090b",
    textMuted: dark ? "#a1a1aa" : "#71717a",
    textFaint: dark ? "#52525b" : "#a1a1aa",
    surface:   dark ? "#1c1c20" : "#ffffff",
    border:    dark ? "rgba(255,255,255,0.14)" : "rgba(0,0,0,0.12)",
    bg:        dark ? "#09090b" : "#fafafa",
  };
  return tokens[name];
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

/** Map asset type -> tab-nav data-tab value, for click-to-filter navigation */
const ASSET_TAB = {
  STOCK:       "stocks",
  BOND:        "bonds",
  MUTUAL_FUND: "mutual-funds",
};

/** Chart.js plugin: draws the total value centered inside a doughnut chart */
const centerTextPlugin = {
  id: "centerText",
  afterDraw(chart) {
    const opts = chart.options.plugins?.centerText;
    if (!opts?.text) return;
    const { ctx, chartArea: { left, right, top, bottom } } = chart;
    const cx = (left + right) / 2;
    const cy = (top + bottom) / 2;
    ctx.save();
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.font = "700 15px " + (getCssVar("--font-sans", "sans-serif").split(",")[0].trim() || "sans-serif");
    ctx.fillStyle = themeTextColor("text");
    ctx.fillText(opts.text, cx, cy - 8);
    ctx.font = "600 10px " + (getCssVar("--font-sans", "sans-serif").split(",")[0].trim() || "sans-serif");
    ctx.fillStyle = themeTextColor("textFaint");
    ctx.fillText("Total Value", cx, cy + 10);
    ctx.restore();
  },
};

/**
 * Render (or re-render) the allocation doughnut chart.
 *
 * @param {string} canvasId  The id of the <canvas> element
 * @param {Array<{type:string, value:number, percent:number, count:number}>} allocationData
 * @param {number} [totalValue]  Optional total value shown centered in the doughnut
 */
export function renderAllocationChart(canvasId, allocationData, totalValue = null) {
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
  const colors      = allocationData.map(d => assetColors[d.type] ?? themeTextColor("textFaint"));

  const legendColor     = themeTextColor("text");
  const tooltipBg       = themeTextColor("surface");
  const tooltipText     = themeTextColor("text");
  const tooltipBorder   = themeTextColor("border");
  const segmentBorder   = themeTextColor("bg");

  const centerLabel = totalValue != null
    ? new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(totalValue)
    : null;

  // Apply theme-aware default text color for all chart plugin text.
  Chart.defaults.color = legendColor;

  _chartInstances[canvasId] = new Chart(canvas, {
    type: "doughnut",
    plugins: [centerTextPlugin],
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
      // Click a segment to jump straight to that asset type's tab.
      onClick(evt, elements) {
        if (!elements.length) return;
        const type = allocationData[elements[0].index]?.type;
        const tabName = ASSET_TAB[type];
        if (!tabName) return;
        document.querySelector(`.tab-nav__btn[data-tab="${tabName}"]`)?.click();
      },
      onHover(evt, elements) {
        evt.native.target.style.cursor = elements.length ? "pointer" : "default";
      },
      plugins: {
        centerText: { text: centerLabel },
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
          // Clicking a legend entry navigates too, same as clicking the wedge.
          onClick(evt, item) {
            const type = allocationData[item.index]?.type;
            const tabName = ASSET_TAB[type];
            if (tabName) document.querySelector(`.tab-nav__btn[data-tab="${tabName}"]`)?.click();
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

/**
 * Render a tiny inline sparkline (SVG polyline) inside a summary card.
 * Pure DOM/SVG — no Chart.js dependency, so it stays cheap for small trend cues.
 *
 * @param {string} svgId  id of the <svg> element (viewBox="0 0 100 28")
 * @param {number[]} values  ordered list of numeric snapshots (oldest → newest)
 * @param {"gain"|"loss"|"neutral"} tone  stroke color tone
 */
export function renderSparkline(svgId, values, tone = "neutral") {
  const svg = document.getElementById(svgId);
  if (!svg) return;

  if (!values || values.length < 2) {
    svg.innerHTML = "";
    return;
  }

  const w = 100, h = 28, pad = 2;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const points = values.map((v, i) => {
    const x = pad + (i / (values.length - 1)) * (w - pad * 2);
    const y = h - pad - ((v - min) / range) * (h - pad * 2);
    return `${x.toFixed(2)},${y.toFixed(2)}`;
  }).join(" ");

  const colorVar = tone === "gain" ? "--color-gain" : tone === "loss" ? "--color-loss" : "--color-accent";
  const stroke = getCssVar(colorVar, "#7c3aed");

  svg.innerHTML = `<polyline points="${points}" style="stroke:${stroke}"></polyline>`;
}

/**
 * Render (or re-render) the Performance Over Time line chart (US-15).
 * Shows portfolio value against cost basis (dashed) across the requested range.
 *
 * @param {string} canvasId  The id of the <canvas> element
 * @param {Array<{date:string, totalValue:number, totalCost:number}>} points
 */
export function renderPerformanceChart(canvasId, points) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;

  if (_chartInstances[canvasId]) {
    _chartInstances[canvasId].destroy();
    delete _chartInstances[canvasId];
  }

  const labels = points.map(p => p.date);
  const values = points.map(p => parseFloat(p.totalValue));
  const costs  = points.map(p => parseFloat(p.totalCost));

  const accent      = getCssVar("--color-accent", "#7c3aed");
  const accentGlow  = getCssVar("--color-accent-glow", "rgba(124,58,237,0.18)");
  const textFaint   = themeTextColor("textFaint");
  const gridColor   = themeTextColor("border");
  const legendColor = themeTextColor("text");
  const tooltipBg     = themeTextColor("surface");
  const tooltipText   = themeTextColor("text");
  const tooltipBorder = themeTextColor("border");

  Chart.defaults.color = legendColor;
  const fmt = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 });

  _chartInstances[canvasId] = new Chart(canvas, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Portfolio Value",
          data: values,
          borderColor: accent,
          backgroundColor: accentGlow,
          fill: true,
          tension: 0.3,
          pointRadius: 0,
          pointHoverRadius: 4,
          borderWidth: 2,
        },
        {
          label: "Cost Basis",
          data: costs,
          borderColor: textFaint,
          borderDash: [5, 4],
          backgroundColor: "transparent",
          fill: false,
          tension: 0.3,
          pointRadius: 0,
          pointHoverRadius: 3,
          borderWidth: 1.5,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: textFaint, maxTicksLimit: 6, font: { size: 11 } },
        },
        y: {
          grid: { color: gridColor },
          ticks: {
            color: textFaint,
            font: { size: 11 },
            callback: v => fmt.format(v),
          },
        },
      },
      plugins: {
        legend: {
          position: "bottom",
          labels: { color: legendColor, font: { size: 12 }, boxWidth: 14 },
        },
        tooltip: {
          backgroundColor: tooltipBg,
          titleColor: tooltipText,
          bodyColor: tooltipText,
          borderColor: tooltipBorder,
          borderWidth: 1,
          callbacks: {
            label(ctx) {
              return ` ${ctx.dataset.label}: ${fmt.format(ctx.parsed.y)}`;
            },
          },
        },
      },
    },
  });
}




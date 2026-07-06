import styles from './LineChart.module.css';

/**
 * A single point on the chart: `t` is the x value (timestamp ms), `y` the metric
 * value, `dateLabel` the human date for the axis.
 */
export interface ChartPoint {
  readonly t: number;
  readonly y: number;
  readonly dateLabel: string;
}

interface LineChartProps {
  /** Chronological points (oldest → newest); needs at least two to draw a line. */
  readonly points: ChartPoint[];
  readonly formatValue: (value: number) => string;
  /** Text alternative describing the trend (accessibility, ADR-006/ADR-010). */
  readonly ariaLabel: string;
}

// viewBox units; the SVG scales to its container width (see CSS).
const VIEW_W = 320;
const VIEW_H = 120;
const PAD = { top: 10, right: 12, bottom: 22, left: 38 };
const PLOT_W = VIEW_W - PAD.left - PAD.right;
const PLOT_H = VIEW_H - PAD.top - PAD.bottom;

/**
 * Minimal inline-SVG line chart for body-progress metrics (FOR-20, ADR-010). No
 * charting library: it maps a measurement series to a polyline with point markers
 * and sparse axis labels. The x-axis is time-proportional, so gaps between distant
 * measurements stay honest (spec FOR-20 edge case) instead of evenly spaced.
 */
export function LineChart({ points, formatValue, ariaLabel }: LineChartProps) {
  const xs = points.map((p) => p.t);
  const ys = points.map((p) => p.y);
  const xMin = Math.min(...xs);
  const xMax = Math.max(...xs);
  const yMin = Math.min(...ys);
  const yMax = Math.max(...ys);
  // Guard against a zero range (e.g. identical timestamps or a flat series).
  const xRange = xMax - xMin || 1;
  const yRange = yMax - yMin || 1;

  const mapX = (t: number) => PAD.left + ((t - xMin) / xRange) * PLOT_W;
  const mapY = (y: number) => PAD.top + (1 - (y - yMin) / yRange) * PLOT_H;

  const polyline = points.map((p) => `${mapX(p.t)},${mapY(p.y)}`).join(' ');
  const last = points[points.length - 1];
  const first = points[0];

  return (
    <svg
      className={styles.chart}
      viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
      preserveAspectRatio="none"
      role="img"
      aria-label={ariaLabel}
    >
      <title>{ariaLabel}</title>

      {/* y-axis min/max labels */}
      <text className={styles.axis} x={PAD.left - 4} y={PAD.top + 3} textAnchor="end">
        {formatValue(yMax)}
      </text>
      <text className={styles.axis} x={PAD.left - 4} y={PAD.top + PLOT_H} textAnchor="end">
        {formatValue(yMin)}
      </text>

      {/* series line + point markers */}
      <polyline className={styles.line} points={polyline} />
      {points.map((p) => (
        <circle
          key={p.t}
          className={p === last ? styles.dotLast : styles.dot}
          cx={mapX(p.t)}
          cy={mapY(p.y)}
          r={p === last ? 3 : 2}
        />
      ))}

      {/* x-axis first/last date labels */}
      <text className={styles.axis} x={PAD.left} y={VIEW_H - 6} textAnchor="start">
        {first.dateLabel}
      </text>
      <text className={styles.axis} x={VIEW_W - PAD.right} y={VIEW_H - 6} textAnchor="end">
        {last.dateLabel}
      </text>
    </svg>
  );
}

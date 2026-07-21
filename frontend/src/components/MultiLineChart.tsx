import styles from './MultiLineChart.module.css';

/** One point: `t` is the x value (timestamp ms), `y` the metric value. */
export interface MultiPoint {
  readonly t: number;
  readonly y: number;
}

/** A named, colored series. `color` is any CSS color (e.g. a `var(--…)`). */
export interface Series {
  readonly label: string;
  readonly color: string;
  readonly points: readonly MultiPoint[];
}

interface MultiLineChartProps {
  readonly series: readonly Series[];
  /** First/last x-axis labels (shared across all series). */
  readonly startLabel: string;
  readonly endLabel: string;
  readonly ariaLabel: string;
}

const VIEW_W = 320;
const VIEW_H = 140;
const PAD = { top: 10, right: 12, bottom: 22, left: 12 };
const PLOT_W = VIEW_W - PAD.left - PAD.right;
const PLOT_H = VIEW_H - PAD.top - PAD.bottom;

/**
 * Multi-series trend chart (FOR-164 dashboard 7-measurement variant:
 * "TENDENCIA 30 DÍAS" overlays weight / body-fat / lean-mass). No chart library
 * (ADR-010), same spirit as {@link LineChart}.
 *
 * <p>Because the series carry different units (kg vs %), each is normalized to
 * its OWN min/max over the shared plot height — this is a trend-shape view
 * (direction over time), not an absolute-value comparison, so a shared numeric
 * y-axis would be misleading and is omitted. The legend names each line and the
 * `ariaLabel` is the text alternative (ui.md accessibility). Purely
 * presentational: it plots the values passed, never derives them (ADR-006).
 */
export function MultiLineChart({ series, startLabel, endLabel, ariaLabel }: MultiLineChartProps) {
  const allX = series.flatMap((s) => s.points.map((p) => p.t));
  const xMin = Math.min(...allX);
  const xMax = Math.max(...allX);
  const xRange = xMax - xMin || 1;
  const mapX = (t: number) => PAD.left + ((t - xMin) / xRange) * PLOT_W;

  return (
    <div className={styles.wrapper}>
      <svg
        className={styles.chart}
        viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={ariaLabel}
      >
        <title>{ariaLabel}</title>
        {series.map((s) => {
          const ys = s.points.map((p) => p.y);
          const yMin = Math.min(...ys);
          const yMax = Math.max(...ys);
          const yRange = yMax - yMin || 1;
          const mapY = (y: number) => PAD.top + (1 - (y - yMin) / yRange) * PLOT_H;
          const polyline = s.points.map((p) => `${mapX(p.t)},${mapY(p.y)}`).join(' ');
          const last = s.points[s.points.length - 1];
          return (
            <g key={s.label}>
              <polyline
                className={styles.line}
                points={polyline}
                style={{ stroke: s.color }}
              />
              {last && (
                <circle cx={mapX(last.t)} cy={mapY(last.y)} r={3} style={{ fill: s.color }} />
              )}
            </g>
          );
        })}
        <text className={styles.axis} x={PAD.left} y={VIEW_H - 6} textAnchor="start">
          {startLabel}
        </text>
        <text className={styles.axis} x={VIEW_W - PAD.right} y={VIEW_H - 6} textAnchor="end">
          {endLabel}
        </text>
      </svg>
      <ul className={styles.legend}>
        {series.map((s) => (
          <li key={s.label} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ backgroundColor: s.color }} aria-hidden="true" />
            {s.label}
          </li>
        ))}
      </ul>
    </div>
  );
}

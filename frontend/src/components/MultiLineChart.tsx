import { Line, LineChart as RechartsLineChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
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

/**
 * Multi-series trend chart (FOR-164 dashboard 7-measurement variant:
 * "TENDENCIA 30 DÍAS" overlays weight / body-fat / lean-mass), rendered with
 * Recharts (ADR-013).
 *
 * <p>Because the series carry different units (kg vs %), each is normalized to
 * its OWN min/max over the shared plot height — this is a trend-shape view
 * (direction over time), not an absolute-value comparison, so a shared numeric
 * y-axis would be misleading and is omitted. Normalizing here rather than
 * declaring one y-axis per series is deliberate: a chart with two live scales
 * invites reading one line as taller than another, which would be meaningless
 * across kg and %. The legend names each line and the `ariaLabel` is the text
 * alternative (ui.md accessibility). Purely presentational: it plots the values
 * passed, never derives them (ADR-006).
 */
export function MultiLineChart({ series, startLabel, endLabel, ariaLabel }: MultiLineChartProps) {
  const data = normalize(series);

  return (
    <div className={styles.wrapper}>
      <div className={styles.chart} role="img" aria-label={ariaLabel}>
        <ResponsiveContainer width="100%" height="100%">
          <RechartsLineChart data={data} margin={{ top: 6, right: 6, bottom: 0, left: 6 }}>
            <XAxis dataKey="t" type="number" scale="time" domain={['dataMin', 'dataMax']} hide />
            {/* Shared 0..1 space: every series was rescaled onto it above. */}
            <YAxis type="number" domain={[0, 1]} hide />
            {series.map((s) => (
              <Line
                key={s.label}
                type="monotone"
                dataKey={s.label}
                stroke={s.color}
                strokeWidth={2}
                strokeLinecap="round"
                dot={false}
                connectNulls
                isAnimationActive={false}
              />
            ))}
          </RechartsLineChart>
        </ResponsiveContainer>
      </div>
      <div className={styles.axisRow} aria-hidden="true">
        <span className={styles.axis}>{startLabel}</span>
        <span className={styles.axis}>{endLabel}</span>
      </div>
      <ul className={styles.legend}>
        {series.map((s) => (
          <li key={s.label} className={styles.legendItem}>
            <span
              className={styles.legendDot}
              style={{ backgroundColor: s.color }}
              aria-hidden="true"
            />
            {s.label}
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * Merges the series into rows keyed by timestamp, each value rescaled to 0..1
 * against its own series' range — the shape-comparison model described above.
 * A series whose values never move sits on the mid-line rather than collapsing
 * onto the floor, which would read as "lowest" instead of "flat".
 */
function normalize(series: readonly Series[]): Record<string, number>[] {
  const rows = new Map<number, Record<string, number>>();

  for (const s of series) {
    const ys = s.points.map((p) => p.y);
    const min = Math.min(...ys);
    const max = Math.max(...ys);
    const range = max - min;
    for (const point of s.points) {
      const row = rows.get(point.t) ?? { t: point.t };
      row[s.label] = range === 0 ? 0.5 : (point.y - min) / range;
      rows.set(point.t, row);
    }
  }

  return [...rows.values()].sort((a, b) => a.t - b.t);
}

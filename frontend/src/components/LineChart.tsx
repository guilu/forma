import { useId } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
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
  /**
   * `detail` (default) is the full card chart: axes, grid and hover tooltip.
   * `spark` is the bare trend line inside a metric tile — no axes, no grid, no
   * hover, because at ~40px tall none of them are legible and the tile's own
   * value and caption already carry the numbers.
   */
  readonly variant?: 'detail' | 'spark';
  /**
   * The series colour, any CSS colour (defaults to the accent). Set it where
   * several charts sit together and each stands for a different metric — the
   * dashboard's body tiles, which borrow the nutrition rings' assignment so a
   * metric keeps one colour across the whole panel. Never the only carrier of
   * that distinction: every chart is titled and carries an `ariaLabel`.
   */
  readonly color?: string;
  /**
   * Pins the x axis to a window instead of letting it span the data's own first
   * and last point. Set it where several charts are stacked and have to be read
   * against each other — the dashboard's trend card, whose three metrics are
   * three charts sharing one pair of date labels. Left unset the axis spans the
   * points it was given, which is what a chart on its own wants.
   */
  readonly xDomain?: readonly [number, number];
}

/** Up to this many x-axis ticks; beyond it the date labels start colliding. */
const MAX_TICKS = 7;

/**
 * Line/area chart for body-progress metrics (FOR-20), rendered with Recharts
 * (ADR-013, which supersedes ADR-010's in-house SVG).
 *
 * <p>The x-axis is a real time scale, not one slot per measurement: gaps
 * between distant measurements stay honest (spec FOR-20 edge case) instead of
 * being evenly spaced. Ticks are taken from the measurements' own timestamps,
 * so every printed date is one on which something was actually measured.
 *
 * <p>Purely presentational: it plots the values it is given and never derives
 * them (ADR-006). Colour comes from the accent token, and the series is also
 * named by `ariaLabel` and by its card title, so the trend is never carried by
 * colour alone.
 */
export function LineChart({
  points,
  formatValue,
  ariaLabel,
  variant = 'detail',
  color = 'var(--color-accent)',
  xDomain,
}: LineChartProps) {
  // One gradient per instance: SVG ids are document-global and several charts
  // are on screen at once (one per metric tile).
  const gradientId = `chart-fill-${useId()}`;
  const spark = variant === 'spark';

  return (
    <div className={spark ? styles.spark : styles.chart} role="img" aria-label={ariaLabel}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart
          data={points}
          margin={
            spark
              ? { top: 2, right: 2, bottom: 2, left: 2 }
              : { top: 8, right: 8, bottom: 0, left: 0 }
          }
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.24} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>

          {!spark && (
            <CartesianGrid
              vertical={false}
              stroke="var(--color-border)"
              strokeDasharray="3 6"
              opacity={0.7}
            />
          )}

          <XAxis
            dataKey="t"
            type="number"
            scale="time"
            domain={xDomain ?? ['dataMin', 'dataMax']}
            hide={spark}
            ticks={axisTicks(points)}
            tickFormatter={(t: number) => labelFor(points, t)}
            tickLine={false}
            axisLine={false}
            tick={{ fill: 'var(--color-text-muted)', fontSize: 11 }}
            tickMargin={8}
            minTickGap={16}
          />
          <YAxis
            dataKey="y"
            type="number"
            domain={['dataMin', 'dataMax']}
            hide={spark}
            tickFormatter={formatValue}
            tickLine={false}
            axisLine={false}
            tick={{ fill: 'var(--color-text-muted)', fontSize: 11 }}
            tickCount={4}
            width={64}
          />

          {!spark && (
            <Tooltip
              content={({ active, payload }) => (
                <ChartTooltip
                  active={active}
                  point={payload?.[0]?.payload as ChartPoint | undefined}
                  formatValue={formatValue}
                />
              )}
              cursor={{ stroke: color, strokeOpacity: 0.35, strokeWidth: 1 }}
            />
          )}

          <Area
            type="monotone"
            dataKey="y"
            stroke={color}
            strokeWidth={2}
            strokeLinecap="round"
            fill={`url(#${gradientId})`}
            /* A dot per measurement turns a dense series into beads on a
               string; only the hovered point gets one. */
            dot={false}
            activeDot={spark ? false : { r: 4, strokeWidth: 0, fill: color }}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

/**
 * Evenly-sampled timestamps taken from the data itself. Letting the library
 * pick its own round numbers would print dates on which nothing was measured —
 * honest-looking labels for points that do not exist.
 */
function axisTicks(points: readonly ChartPoint[]): number[] {
  if (points.length <= MAX_TICKS) {
    return points.map((p) => p.t);
  }
  const step = (points.length - 1) / (MAX_TICKS - 1);
  return Array.from({ length: MAX_TICKS }, (_, i) => points[Math.round(i * step)].t);
}

function labelFor(points: readonly ChartPoint[], t: number): string {
  return points.find((p) => p.t === t)?.dateLabel ?? '';
}

/**
 * Tooltip in the app's own surface tokens rather than the library's white
 * default, which is unreadable in the dark theme.
 */
function ChartTooltip({
  active,
  point,
  formatValue,
}: {
  readonly active?: boolean;
  readonly point?: ChartPoint;
  readonly formatValue: (value: number) => string;
}) {
  if (!active || !point) {
    return null;
  }
  return (
    <div className={styles.tooltip}>
      <p className={styles.tooltipValue}>{formatValue(point.y)}</p>
      <p className={styles.tooltipDate}>{point.dateLabel}</p>
    </div>
  );
}

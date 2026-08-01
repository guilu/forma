import type { ChartPoint } from '../components/LineChart';

/**
 * Time-range filtering for a body-metric chart, shared by the measurements page
 * and the dashboard's Evolución widget (FOR-188).
 *
 * <p>It lived privately in `MeasurementsPage` while only that screen had working
 * range buttons; the dashboard widget rendered the same tabs inert. Extracted
 * rather than copied so the two cannot drift — and so the "only offer a range
 * that narrows the view" rule below has one definition, not two.
 *
 * <p>The option *lists* stay with each caller: the page offers 7D/1M/3M/6M/1A
 * and the dashboard card a trimmed 7D/30D/Todos, because five buttons do not
 * fit a widget. Only the filtering is shared.
 */
export interface RangeOption {
  readonly key: string;
  readonly label: string;
  /** Window in days, or `null` for "everything". */
  readonly days: number | null;
}

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Points within `days` of the latest point; the full series when `days` is
 * `null`. Measured back from the newest measurement rather than from today, so a
 * user who stopped logging still sees their last week of data instead of an
 * empty chart.
 */
export function pointsInRange(points: ChartPoint[], days: number | null): ChartPoint[] {
  if (days === null || points.length === 0) {
    return points;
  }
  const latestT = points[points.length - 1].t;
  const cutoff = latestT - days * DAY_MS;
  return points.filter((point) => point.t >= cutoff);
}

/**
 * The options worth showing for this series (spec FOR-52: "cap chart range
 * options to the data actually available").
 *
 * <p>A window is offered only when it leaves at least two points to draw a line
 * with *and* fewer than the whole history — otherwise the button would render
 * the exact same chart as "everything", which is not a choice. The `null` option
 * is always offered.
 */
export function narrowingRanges(
  points: ChartPoint[],
  options: readonly RangeOption[],
): readonly RangeOption[] {
  return options.filter((option) => {
    if (option.days === null) {
      return true;
    }
    const filtered = pointsInRange(points, option.days);
    return filtered.length >= 2 && filtered.length < points.length;
  });
}

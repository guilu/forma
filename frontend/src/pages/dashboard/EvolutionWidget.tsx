import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import { narrowingRanges, pointsInRange, type RangeOption } from '../chartRanges';
import { WidgetSection } from './WidgetSection';
import styles from './EvolutionWidget.module.css';

/**
 * "Evolución" widget (FOR-164 dashboard 7-measurement variant): a single-metric
 * body trend the user can switch between Peso / Grasa / Músculo, with the
 * latest value highlighted. Real FOR-17 measurement history (ADR-006).
 *
 * <p>The metric selector and the range tabs are both real. The tabs used to be
 * inert decoration, on the grounds that no endpoint takes a date range — but the
 * range never needed one: the full history is already in hand, and the
 * measurements page had been filtering it client-side since FOR-52. This widget
 * now uses that same shared filtering (`pages/chartRanges.ts`) instead of
 * rendering buttons that do nothing.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly history: BodyMeasurement[] };

type MetricKey = 'weight' | 'fat' | 'lean';

const METRICS: Record<
  MetricKey,
  { label: string; unit: string; select: (m: BodyMeasurement) => number | undefined }
> = {
  weight: { label: 'Peso', unit: 'kg', select: (m) => m.weightKg },
  fat: { label: 'Grasa', unit: '%', select: (m) => m.bodyFatPercentage },
  lean: { label: 'Músculo', unit: 'kg', select: (m) => m.leanMassKg },
};

/**
 * Three tabs rather than the measurements page's five: five buttons do not fit a
 * widget card, so the set is trimmed to the two windows a user actually asks for
 * plus "everything". The filtering itself is shared with that page
 * (`pages/chartRanges.ts`) so the two cannot drift.
 */
const RANGES: readonly RangeOption[] = [
  { key: '7D', label: '7D', days: 7 },
  { key: '30D', label: '30D', days: 30 },
  { key: 'ALL', label: 'Todos', days: null },
];

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function EvolutionWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [metric, setMetric] = useState<MetricKey>('weight');
  // Keyed by option key, not index: the offered set changes with the data, and
  // the choice has to survive a metric switch (a different series can offer
  // different windows).
  const [rangeKey, setRangeKey] = useState('ALL');

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (!active) return;
        setState(
          measurements.length === 0
            ? { status: 'empty' }
            : { status: 'ready', history: measurements },
        );
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  const selector =
    state.status === 'ready' ? (
      <label className={styles.metricSelect}>
        <span className={styles.srOnly}>Métrica</span>
        <select value={metric} onChange={(e) => setMetric(e.target.value as MetricKey)}>
          {(Object.keys(METRICS) as MetricKey[]).map((key) => (
            <option key={key} value={key}>
              {METRICS[key].label}
            </option>
          ))}
        </select>
      </label>
    ) : undefined;

  return (
    <WidgetSection id="evolution-widget-title" title="Evolución" action={selector}>
      {renderContent(state, metric, rangeKey, setRangeKey)}
    </WidgetSection>
  );
}

function renderContent(
  state: State,
  metric: MetricKey,
  rangeKey: string,
  onRangeChange: (key: string) => void,
) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu evolución…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu evolución. Inténtalo de nuevo más tarde." />;
  }
  if (state.status === 'empty') {
    return (
      <EmptyState variant="filtered" title="Aún no hay mediciones para mostrar tu evolución." />
    );
  }

  const { label, unit, select } = METRICS[metric];
  const chrono = [...state.history].reverse();

  const allPoints: ChartPoint[] = chrono.flatMap((m) => {
    const value = select(m);
    return value === undefined
      ? []
      : [{ t: Date.parse(m.measuredAt), y: value, dateLabel: formatDate(m.measuredAt) }];
  });
  const ranges = narrowingRanges(allPoints, RANGES);
  // The stored choice may not be on offer for this series (a metric with fewer
  // points offers fewer windows); fall back to the last option, always "todos".
  const active = ranges.find((range) => range.key === rangeKey) ?? ranges[ranges.length - 1];
  const points = pointsInRange(allPoints, active.days);
  // The headline stays the newest measurement of the metric, not of the window:
  // it answers "where am I now", which the range does not change.
  const latest = allPoints[allPoints.length - 1];

  return (
    <div className={styles.card}>
      <p className={styles.value}>
        {latest ? latest.y.toFixed(1) : '—'}
        <span className={styles.unit}> {unit}</span>
      </p>
      {points.length >= 2 ? (
        // The chart takes whatever height the card has left over, rather than a
        // fixed 140px that leaves a band of empty card under it.
        <div className={styles.chartArea}>
          <LineChart
            points={points}
            formatValue={(v) => `${v.toFixed(1)} ${unit}`}
            ariaLabel={`Evolución de ${label.toLowerCase()}: ${points.length} mediciones.`}
          />
        </div>
      ) : (
        <p className={styles.hint}>Registra más mediciones para ver la curva de evolución.</p>
      )}
      {/* One option is not a choice: with too little history to narrow, the
          group would render a single button that cannot change the chart. */}
      {ranges.length > 1 && (
        <div className={styles.ranges} role="group" aria-label="Rango del gráfico">
          {ranges.map((range) => (
            <button
              key={range.key}
              type="button"
              className={range.key === active.key ? styles.rangeActive : styles.range}
              aria-pressed={range.key === active.key}
              onClick={() => onRangeChange(range.key)}
            >
              {range.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

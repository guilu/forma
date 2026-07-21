import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import { WidgetSection } from './WidgetSection';
import styles from './EvolutionWidget.module.css';

/**
 * "Evolución" widget (FOR-164 dashboard 7-measurement variant): a single-metric
 * body trend the user can switch between Peso / Grasa / Músculo, with the
 * latest value highlighted. Real FOR-17 measurement history (ADR-006).
 *
 * <p>The metric selector is real (re-plots a different backed series). The
 * range tabs (7D / 30D / 90D / 1A / Todo) are visual-only: no body-measurement
 * endpoint takes a date-range parameter, so they're inert affordances — the
 * chart always shows the full returned history. Documented gap, not invented.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly history: BodyMeasurement[] };

type MetricKey = 'weight' | 'fat' | 'lean';

const METRICS: Record<
  MetricKey,
  { label: string; unit: string; select: (m: BodyMeasurement) => number }
> = {
  weight: { label: 'Peso', unit: 'kg', select: (m) => m.weightKg },
  fat: { label: 'Grasa', unit: '%', select: (m) => m.bodyFatPercentage },
  lean: { label: 'Músculo', unit: 'kg', select: (m) => m.leanMassKg },
};

const RANGES = ['7D', '30D', '90D', '1A', 'Todo'] as const;

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function EvolutionWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [metric, setMetric] = useState<MetricKey>('weight');

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
      {renderContent(state, metric)}
    </WidgetSection>
  );
}

function renderContent(state: State, metric: MetricKey) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu evolución…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu evolución. Inténtalo de nuevo más tarde." />;
  }
  if (state.status === 'empty') {
    return <EmptyState variant="filtered" title="Aún no hay mediciones para mostrar tu evolución." />;
  }

  const { label, unit, select } = METRICS[metric];
  const chrono = [...state.history].reverse();
  const latest = state.history[0];

  const points: ChartPoint[] = chrono.map((m) => ({
    t: Date.parse(m.measuredAt),
    y: select(m),
    dateLabel: formatDate(m.measuredAt),
  }));

  return (
    <div className={styles.card}>
      <p className={styles.value}>
        {select(latest).toFixed(1)}
        <span className={styles.unit}> {unit}</span>
      </p>
      {points.length >= 2 ? (
        <LineChart
          points={points}
          formatValue={(v) => `${v.toFixed(1)} ${unit}`}
          ariaLabel={`Evolución de ${label.toLowerCase()}: ${points.length} mediciones.`}
        />
      ) : (
        <p className={styles.hint}>Registra más mediciones para ver la curva de evolución.</p>
      )}
      {/* Range tabs — visual only (no date-range endpoint). */}
      <div className={styles.ranges} aria-hidden="true">
        {RANGES.map((r) => (
          <span key={r} className={r === '30D' ? styles.rangeActive : styles.range}>
            {r}
          </span>
        ))}
      </div>
    </div>
  );
}

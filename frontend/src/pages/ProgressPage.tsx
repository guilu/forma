import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { LineChart, type ChartPoint } from '../components/LineChart';
import { LoadingState } from '../components/LoadingState';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { InsightsHistorySection } from './progress/InsightsHistorySection';
import { InsightsSection } from './progress/InsightsSection';
import { ProgressPhotosSection } from './progress/ProgressPhotosSection';
import styles from './ProgressPage.module.css';

/**
 * Progress page (FOR-20). Renders simple body-progress line graphs (weight, body
 * fat %, lean mass) from the FOR-17 API using in-house SVG charts (ADR-010 — no
 * chart library). Read-only, recent-window only; handles loading, empty and error
 * states. Values come straight from the API (ADR-006).
 *
 * <p>Also hosts the FOR-144 progress-photos gallery/upload ({@link
 * ProgressPhotosSection}, mockup: "FOTOS DE PROGRESO" in
 * `docs/6-progreso.png`), the FOR-56 insights & recommendations surface
 * ({@link InsightsSection}) and the FOR-124 past-weeks history ({@link
 * InsightsHistorySection}) below the measurement charts. This is where the
 * fuller insights view lives for the MVP — the nav has no dedicated "Insights"
 * route (`frontend/src/app/navigation.ts`), per the FOR-56 spec's Open
 * Question (preserved by FOR-124), so no new nav item/route was added; the
 * dashboard keeps its own compact summary (FOR-51 `InsightWidget`). Each
 * section fetches independently, so a measurements, photos, current-week or
 * history failure never blocks the others.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly measurements: BodyMeasurement[] };

/** How many recent measurements the default view plots (documented; no date filters in MVP). */
const RECENT_WINDOW = 12;

interface MetricConfig {
  readonly label: string;
  readonly unit: string;
  readonly value: (m: BodyMeasurement) => number | undefined;
}

const METRICS: readonly MetricConfig[] = [
  { label: 'Evolución de peso', unit: 'kg', value: (m) => m.weightKg },
  { label: 'Evolución de grasa corporal', unit: '%', value: (m) => m.bodyFatPercentage },
  { label: 'Evolución de masa magra', unit: 'kg', value: (m) => m.leanMassKg },
];

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

function formatValue(value: number, unit: string): string {
  return `${value.toFixed(1)} ${unit}`;
}

export function ProgressPage() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) {
          setState({ status: 'ready', measurements });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Progreso</h1>
        <p className={styles.subtitle}>Tu evolución, tus resultados.</p>
      </header>
      {renderContent(state)}
      <ProgressPhotosSection />
      <InsightsSection />
      <InsightsHistorySection />
    </div>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tu evolución…" />;
  }

  if (state.status === 'error') {
    // No retry offered here — matches this page's pre-existing behavior
    // (no reload callback is exposed from its load effect); FOR-60 only
    // standardizes state rendering, not data-fetching behavior.
    return <ErrorState message="No se pudo cargar tu evolución. Inténtalo de nuevo más tarde." />;
  }

  if (state.measurements.length === 0) {
    return (
      <EmptyState
        title="Aún no hay mediciones."
        action={<Link to="/mediciones">Registra tu primera medición</Link>}
      />
    );
  }

  // Newest-first from the API → take the most recent window and plot chronologically.
  const recent = state.measurements.slice(0, RECENT_WINDOW).reverse();

  return (
    <section className={styles.grid}>
      {METRICS.map((metric) => (
        <MetricChart key={metric.label} metric={metric} measurements={recent} />
      ))}
    </section>
  );
}

function MetricChart({
  metric,
  measurements,
}: {
  readonly metric: MetricConfig;
  readonly measurements: BodyMeasurement[];
}) {
  const points: ChartPoint[] = measurements
    .map((m) => ({ value: metric.value(m), measuredAt: m.measuredAt }))
    .filter((p): p is { value: number; measuredAt: string } => typeof p.value === 'number')
    .map((p) => ({ t: Date.parse(p.measuredAt), y: p.value, dateLabel: formatDate(p.measuredAt) }));

  const latest = points[points.length - 1];

  return (
    <Card title={metric.label} headingLevel={2}>
      {points.length < 2 ? (
        <div className={styles.single}>
          {latest ? (
            <p className={styles.singleValue}>{formatValue(latest.y, metric.unit)}</p>
          ) : null}
          <EmptyState
            variant="filtered"
            title="Necesitas al menos dos mediciones para ver la evolución."
          />
        </div>
      ) : (
        <>
          <p className={styles.latest}>
            {formatValue(latest.y, metric.unit)}{' '}
            <span className={styles.latestDate}>· {latest.dateLabel}</span>
          </p>
          <LineChart
            points={points}
            formatValue={(v) => v.toFixed(1)}
            ariaLabel={`${metric.label}: ${points.length} mediciones, de ${formatValue(
              points[0].y,
              metric.unit,
            )} a ${formatValue(latest.y, metric.unit)}.`}
          />
        </>
      )}
    </Card>
  );
}

import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { MultiLineChart, type Series } from '../../components/MultiLineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import { WidgetSection } from './WidgetSection';
import styles from './TrendWidget.module.css';

/**
 * "Tendencia 30 días" widget (FOR-164 dashboard mockup): the recent weight /
 * body-fat / lean-mass trends from FOR-17 measurements, overlaid as a
 * multi-series {@link MultiLineChart}.
 *
 * <p>The window is thirty *days*, ending today — which is what the card has
 * always been titled. It used to be the last thirty *measurements* (a
 * `slice(0, 30)` over the newest-first list), so an account with hundreds of
 * rows saw ten months of history under a label promising one, with the actual
 * recent month crushed into the last few pixels.
 *
 * <p>The axis is labelled with the window's own bounds rather than with the
 * first and last measurement in it: the chart answers "the last 30 days", and
 * the dates should say so even when the data starts partway through. With
 * nothing to plot inside the window it says that, instead of falling back to
 * older data the title does not cover (ADR-006 — no fabricated trend).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly history: BodyMeasurement[] };

const WINDOW_DAYS = 30;
const DAY_MS = 24 * 60 * 60 * 1000;

function toPoints(
  measurements: readonly BodyMeasurement[],
  select: (m: BodyMeasurement) => number | undefined,
) {
  return measurements.flatMap((m) => {
    const value = select(m);
    return value === undefined ? [] : [{ t: Date.parse(m.measuredAt), y: value }];
  });
}

function formatTimestamp(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function TrendWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) setState({ status: 'ready', history: measurements });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="trend-widget-title" title="Tendencia 30 días">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu tendencia…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu tendencia. Inténtalo de nuevo más tarde." />;
  }

  const now = Date.now();
  const from = now - WINDOW_DAYS * DAY_MS;
  // Newest-first from the API → the window, plotted chronologically.
  const window = state.history
    .filter((measurement) => Date.parse(measurement.measuredAt) >= from)
    .reverse();

  if (window.length < 2) {
    return (
      <p className={styles.empty} role="status">
        No hay mediciones en los últimos {WINDOW_DAYS} días. Sigue registrando tus mediciones para
        ver tu evolución.
      </p>
    );
  }

  /*
   * Colour per series, matching the design's assignment (FOR-188): weight green,
   * body fat blue, lean mass amber. Blue and amber swapped from what shipped —
   * fat was on the warning token and muscle on info, which read as "fat is a
   * warning" and left the brand green next to a blue nobody had chosen.
   * Every series is also named in the legend and in the chart's `ariaLabel`, so
   * none of this is carried by colour alone.
   */
  const series: Series[] = [
    {
      label: 'Peso (kg)',
      color: 'var(--color-accent)',
      points: toPoints(window, (m) => m.weightKg),
    },
    {
      label: 'Grasa (%)',
      color: 'var(--color-info)',
      points: toPoints(window, (m) => m.bodyFatPercentage),
    },
    {
      label: 'Músculo (kg)',
      color: 'var(--color-warning-graphic)',
      points: toPoints(window, (m) => m.leanMassKg),
    },
  ].filter((s) => s.points.length > 0);

  return (
    <MultiLineChart
      series={series}
      startLabel={formatTimestamp(from)}
      endLabel={formatTimestamp(now)}
      ariaLabel={`Tendencia de peso, grasa corporal y masa muscular en los últimos ${WINDOW_DAYS} días: ${window.length} mediciones.`}
    />
  );
}

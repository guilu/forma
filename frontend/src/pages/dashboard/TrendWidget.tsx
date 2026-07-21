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
 * multi-series {@link MultiLineChart}. When there aren't enough measurements to
 * plot a line yet (the "1-measurement" variant), it shows the same honest "not
 * enough data" copy the mockup itself renders — no fabricated trend (ADR-006).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly history: BodyMeasurement[] };

const WINDOW = 30;

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
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

  // Newest-first from the API → most recent window, plotted chronologically.
  const window = state.history.slice(0, WINDOW).reverse();

  if (window.length < 2) {
    return (
      <p className={styles.empty}>
        Aún no hay suficientes datos para mostrar la tendencia. Sigue registrando tus mediciones
        para ver tu evolución.
      </p>
    );
  }

  const series: Series[] = [
    {
      label: 'Peso (kg)',
      color: 'var(--color-accent)',
      points: window.map((m) => ({ t: Date.parse(m.measuredAt), y: m.weightKg })),
    },
    {
      label: 'Grasa (%)',
      color: 'var(--color-warning)',
      points: window.map((m) => ({ t: Date.parse(m.measuredAt), y: m.bodyFatPercentage })),
    },
    {
      label: 'Músculo (kg)',
      color: 'var(--color-info, #3b82f6)',
      points: window.map((m) => ({ t: Date.parse(m.measuredAt), y: m.leanMassKg })),
    },
  ];

  return (
    <MultiLineChart
      series={series}
      startLabel={formatDate(window[0].measuredAt)}
      endLabel={formatDate(window[window.length - 1].measuredAt)}
      ariaLabel={`Tendencia de peso, grasa corporal y masa muscular en las últimas ${window.length} mediciones.`}
    />
  );
}

import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import { WidgetSection } from './WidgetSection';
import styles from './TrendWidget.module.css';

/**
 * "Tendencia 30 días" widget (FOR-164 dashboard mockup): the recent weight
 * trend from FOR-17 measurements. When there aren't enough measurements to plot
 * a line yet, it shows the same honest "not enough data" copy the mockup itself
 * renders — no fabricated trend (ADR-006). Presentational only.
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

  const points: ChartPoint[] = state.history
    .slice(0, WINDOW)
    .reverse()
    .map((m) => ({ t: Date.parse(m.measuredAt), y: m.weightKg, dateLabel: formatDate(m.measuredAt) }));

  if (points.length < 2) {
    return (
      <p className={styles.empty}>
        Aún no hay suficientes datos para mostrar la tendencia. Sigue registrando tus mediciones
        para ver tu evolución.
      </p>
    );
  }

  return (
    <LineChart
      points={points}
      formatValue={(v) => `${v.toFixed(1)} kg`}
      ariaLabel={`Tendencia de peso: ${points.length} mediciones recientes, de ${points[0].y.toFixed(1)} kg a ${points[points.length - 1].y.toFixed(1)} kg.`}
    />
  );
}

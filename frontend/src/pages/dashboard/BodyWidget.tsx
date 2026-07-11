import { useEffect, useState } from 'react';
import { MetricCard } from '../../components/MetricCard';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import { WidgetSection } from './WidgetSection';
import styles from './BodyWidget.module.css';

/**
 * Body composition summary widget (FOR-51): PESO / GRASA CORPORAL / MASA MUSCULAR / IMC
 * from the latest FOR-17 measurement, plus a weight sparkline over the recent window
 * (mirrors `ProgressPage`'s FOR-20 charting). Presentational only — reads the API values
 * as returned, never recomputes them (ADR-006).
 *
 * <p>The mockup (`docs/1-dashboard.png`) also shows a "vs semana pasada" delta per card.
 * That delta is the FOR-21 `WeeklyBodySummary` computation, which is explicitly not
 * exposed over HTTP (`specs/FOR-21/spec.md`: "no new HTTP endpoint is required by this
 * story"). Recomputing a "nearest prior-week measurement" comparison here would
 * duplicate that domain rule in the UI (forbidden by ADR-001/AGENTS.md), so the delta is
 * omitted rather than invented — documented gap, see FOR-51 PR "Known limitations".
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | {
      readonly status: 'ready';
      readonly latest: BodyMeasurement;
      readonly history: BodyMeasurement[];
    };

const SPARKLINE_WINDOW = 8;

function format(value: number | undefined): string {
  return value === undefined ? '—' : value.toFixed(1);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function BodyWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (!active) return;
        if (measurements.length === 0) {
          setState({ status: 'empty' });
          return;
        }
        setState({ status: 'ready', latest: measurements[0], history: measurements });
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
    <WidgetSection id="body-widget-title" title="Composición corporal" linkTo="/mediciones">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu composición corporal…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudo cargar tu composición corporal. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  if (state.status === 'empty') {
    return (
      <p className={styles.message} role="status">
        Aún no hay mediciones. Registra tu primera medición para ver tu resumen.
      </p>
    );
  }

  const { latest, history } = state;

  // Newest-first from the API → take the most recent window and plot chronologically.
  const points: ChartPoint[] = history
    .slice(0, SPARKLINE_WINDOW)
    .reverse()
    .map((m) => ({
      t: Date.parse(m.measuredAt),
      y: m.weightKg,
      dateLabel: formatDate(m.measuredAt),
    }));

  return (
    <>
      <div className={styles.grid}>
        <MetricCard label="Peso" value={format(latest.weightKg)} unit="kg" />
        <MetricCard label="Grasa corporal" value={format(latest.bodyFatPercentage)} unit="%" />
        <MetricCard label="Masa muscular" value={format(latest.leanMassKg)} unit="kg" />
        <MetricCard label="IMC" value={format(latest.bmi)} />
      </div>
      {points.length >= 2 && (
        <div className={styles.sparkline}>
          <LineChart
            points={points}
            formatValue={(v) => `${v.toFixed(1)} kg`}
            ariaLabel={`Evolución de peso: ${points.length} mediciones recientes, de ${points[0].y.toFixed(1)} kg a ${points[points.length - 1].y.toFixed(1)} kg.`}
          />
        </div>
      )}
    </>
  );
}

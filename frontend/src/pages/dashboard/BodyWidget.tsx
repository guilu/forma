import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { MetricCard } from '../../components/MetricCard';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';
import styles from './BodyWidget.module.css';

/**
 * Body composition metrics row (FOR-51, rebuilt for the FOR-164 dashboard
 * mockup): PESO / GRASA CORPORAL / MASA MUSCULAR / IMC tiles, each with its own
 * recent sparkline and a "{n} medición(es)" caption, from the latest FOR-17
 * measurement. Presentational only — reads API values as returned (ADR-006).
 *
 * <p>Unlike the earlier version this no longer wraps itself in a
 * `WidgetSection` heading: in the new mockup these are the first summary tiles
 * of the page's metrics row (alongside CALORÍAS / AGUA), not a titled section,
 * so the page provides the (sr-only) row heading and each {@link MetricCard}
 * keeps its own tile title.
 *
 * <p>The mockup's per-tile "vs semana pasada" delta ("–Sin cambios") is the
 * FOR-21 `WeeklyBodySummary` computation, which is not exposed over HTTP;
 * recomputing it in the UI would duplicate a domain rule (ADR-001), so the
 * caption honestly shows the measurement count instead of an invented delta.
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

  return <div className={styles.body}>{renderContent(state)}</div>;
}

/** Builds a chronological sparkline series for one numeric metric selector. */
function sparkline(
  history: BodyMeasurement[],
  select: (m: BodyMeasurement) => number | undefined,
): ChartPoint[] {
  return history
    .slice(0, SPARKLINE_WINDOW)
    .reverse()
    .flatMap((m) => {
      const y = select(m);
      return y === undefined
        ? []
        : [{ t: Date.parse(m.measuredAt), y, dateLabel: formatDate(m.measuredAt) }];
    });
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <div className={styles.full}>
        <WidgetLoading label="Cargando tu composición corporal…" rows={2} />
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className={styles.full}>
        <ErrorState message="No se pudo cargar tu composición corporal. Inténtalo de nuevo más tarde." />
      </div>
    );
  }

  if (state.status === 'empty') {
    return (
      <div className={styles.full}>
        <EmptyState
          variant="filtered"
          title="Aún no hay mediciones. Registra tu primera medición para ver tu resumen."
        />
      </div>
    );
  }

  const { latest, history } = state;
  const caption = `${history.length} ${history.length === 1 ? 'medición' : 'mediciones'}`;

  const tiles = [
    { label: 'Peso', value: format(latest.weightKg), unit: 'kg', select: (m: BodyMeasurement) => m.weightKg },
    {
      label: 'Grasa corporal',
      value: format(latest.bodyFatPercentage),
      unit: '%',
      select: (m: BodyMeasurement) => m.bodyFatPercentage,
    },
    {
      label: 'Masa muscular',
      value: format(latest.leanMassKg),
      unit: 'kg',
      select: (m: BodyMeasurement) => m.leanMassKg,
    },
    { label: 'IMC', value: format(latest.bmi), unit: undefined, select: (m: BodyMeasurement) => m.bmi },
  ];

  return (
    <>
      {tiles.map((tile) => {
        const points = sparkline(history, tile.select);
        return (
          <MetricCard
            key={tile.label}
            label={tile.label}
            value={tile.value}
            unit={tile.unit}
            caption={caption}
            trend={
              points.length >= 2 ? (
                <LineChart
                  points={points}
                  formatValue={(v) => `${v.toFixed(1)}${tile.unit ? ` ${tile.unit}` : ''}`}
                  ariaLabel={`Evolución de ${tile.label.toLowerCase()}: ${points.length} mediciones recientes.`}
                />
              ) : undefined
            }
          />
        );
      })}
    </>
  );
}

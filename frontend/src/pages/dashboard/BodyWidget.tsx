import { ButtonLink } from '../../components/ButtonLink';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { MetricCard } from '../../components/MetricCard';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { type BodyMeasurement } from '../../api/bodyMeasurements';
import styles from './BodyWidget.module.css';

/**
 * Body composition metrics row (FOR-51, rebuilt for the FOR-164 dashboard
 * mockup): PESO / GRASA CORPORAL / MASA MUSCULAR / IMC tiles, each with its own
 * recent sparkline and a "{n} medición(es)" caption, from the latest FOR-17
 * measurement. Presentational only — reads API values as returned (ADR-006).
 *
 * <p>FOR-189 shortened two of the four labels ("Grasa corporal" → "Grasa",
 * "Masa muscular" → "Músculo"). Four tiles in a row read as a set, and the long
 * forms wrapped onto a second line while "Peso" and "IMC" did not — the row
 * looked ragged for no gain in meaning. "Músculo" still labels `leanMassKg`
 * (lean mass; the domain has no separate muscle-mass field), same as before.
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
/**
 * The measurement list is owned by {@link DashboardPage}, not fetched here
 * (FOR-189): the header's date navigator selects which measurement these tiles
 * show, and a selection cannot be shared between two components that each fetch
 * their own copy. This widget is presentational — it renders whichever state it
 * is handed.
 */
export type BodyState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | {
      readonly status: 'ready';
      /** Newest first, as the API returns it. */
      readonly history: BodyMeasurement[];
      /** Index into `history` of the measurement the tiles describe. */
      readonly selected: number;
    };

const SPARKLINE_WINDOW = 8;

function format(value: number | undefined): string {
  return value === undefined ? '—' : value.toFixed(1);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function BodyWidget({ state }: { readonly state: BodyState }) {
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

function renderContent(state: BodyState) {
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
          action={
            // A link, not a button: the entry form lives on the measurements
            // page and mounting a second copy of it here would give the same
            // flow two homes. Styled as the primary action so it reads as the
            // same offer the measurements page makes.
            <ButtonLink to="/app/measurements">+ Registrar medición</ButtonLink>
          }
        />
      </div>
    );
  }

  const { history, selected } = state;
  const current = history[selected];
  /*
   * The sparkline runs up to the selected measurement, not past it: it is the
   * run-up to the number on the tile, so a line continuing into later dates
   * would describe something the tile is not showing.
   */
  const upToSelected = history.slice(selected);
  const caption = `${upToSelected.length} ${upToSelected.length === 1 ? 'medición' : 'mediciones'}`;

  const tiles = [
    {
      label: 'Peso',
      value: format(current.weightKg),
      unit: 'kg',
      select: (m: BodyMeasurement) => m.weightKg,
    },
    {
      label: 'Grasa',
      value: format(current.bodyFatPercentage),
      unit: '%',
      select: (m: BodyMeasurement) => m.bodyFatPercentage,
    },
    {
      label: 'Músculo',
      value: format(current.leanMassKg),
      unit: 'kg',
      select: (m: BodyMeasurement) => m.leanMassKg,
    },
    {
      label: 'IMC',
      value: format(current.bmi),
      unit: undefined,
      select: (m: BodyMeasurement) => m.bmi,
    },
  ];

  return (
    <>
      {tiles.map((tile) => {
        const points = sparkline(upToSelected, tile.select);
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
                  variant="spark"
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

import { ButtonLink } from '../../components/ButtonLink';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { MetricCard } from '../../components/MetricCard';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import { type BodyMeasurement } from '../../api/bodyMeasurements';
import { change as formatChange, fixed } from '../../format/measures';
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
 * <p>One colour per metric, borrowed from the nutrition rings so the panel
 * speaks with one voice: weight keeps the brand green, fat takes the rings'
 * amber, muscle their blue and BMI their violet. The colour is decoration on
 * top of a tile that already prints its own name, value and unit.
 *
 * <p>Each tile carries the change from the measurement before the selected one,
 * bracketed beside the value: "73.6 kg (-0.5)". That is a subtraction of two
 * numbers the API already returned, not the mockup's "vs semana pasada" — which
 * is the FOR-21 `WeeklyBodySummary` computation, a domain rule that is not
 * exposed over HTTP and would be duplicated by recomputing it here (ADR-001).
 * The caption keeps saying how many measurements there are. With nothing before
 * the selected one, or with the metric missing from it, the tile shows no change
 * rather than an invented zero (ADR-006).
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
  return value === undefined ? '—' : fixed(value);
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

/** `undefined` where there is nothing to compare against — never a zero. */
function difference(current: number | undefined, previous: number | undefined) {
  return current === undefined || previous === undefined ? undefined : current - previous;
}

/** «0.5 kg menos que la medición anterior» — lo que «(-0.5)» no dice en voz alta. */
function describe(change: number, unit: string | undefined): string {
  const size = fixed(Math.abs(change));
  const magnitude = unit === undefined ? size : `${size} ${unit}`;
  if (change === 0) {
    return 'Igual que la medición anterior';
  }
  return `${magnitude} ${change > 0 ? 'más' : 'menos'} que la medición anterior`;
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

  /*
   * La medición inmediatamente anterior a la seleccionada. El historial viene de
   * más nueva a más vieja, así que «la anterior» es la siguiente de la lista.
   */
  const previous = history[selected + 1];

  const tiles = [
    {
      label: 'Peso',
      value: format(current.weightKg),
      unit: 'kg',
      /*
       * La unidad con la que se narra la diferencia, que no siempre es la del
       * valor: la grasa se mide en %, pero su variación son PUNTOS de ese
       * porcentaje — decir «0.5 % menos» sería un porcentaje de un porcentaje.
       */
      deltaUnit: 'kg',
      color: 'var(--color-accent)',
      select: (m: BodyMeasurement) => m.weightKg,
    },
    {
      label: 'Grasa',
      value: format(current.bodyFatPercentage),
      unit: '%',
      deltaUnit: 'puntos',
      color: 'var(--color-warning-graphic)',
      select: (m: BodyMeasurement) => m.bodyFatPercentage,
    },
    {
      label: 'Músculo',
      value: format(current.leanMassKg),
      unit: 'kg',
      deltaUnit: 'kg',
      color: 'var(--color-info)',
      select: (m: BodyMeasurement) => m.leanMassKg,
    },
    {
      label: 'IMC',
      value: format(current.bmi),
      unit: undefined,
      deltaUnit: undefined,
      color: 'var(--color-violet)',
      select: (m: BodyMeasurement) => m.bmi,
    },
  ];

  return (
    <>
      {tiles.map((tile) => {
        const points = sparkline(upToSelected, tile.select);
        const change = difference(tile.select(current), previous && tile.select(previous));
        return (
          <MetricCard
            key={tile.label}
            label={tile.label}
            value={tile.value}
            unit={tile.unit}
            delta={change === undefined ? undefined : `(${formatChange(change)})`}
            deltaDescription={change === undefined ? undefined : describe(change, tile.deltaUnit)}
            caption={caption}
            trend={
              points.length >= 2 ? (
                <LineChart
                  variant="spark"
                  points={points}
                  color={tile.color}
                  formatValue={(v) => `${fixed(v)}${tile.unit ? ` ${tile.unit}` : ''}`}
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

import { ErrorState } from '../../components/ErrorState';
import { LineChart, type ChartPoint } from '../../components/LineChart';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { BodyMeasurement } from '../../api/bodyMeasurements';
import { change as formatChange, fixed } from '../../format/measures';
import type { MeasurementsState } from './measurementsState';
import { WidgetSection } from './WidgetSection';
import styles from './TrendWidget.module.css';

/**
 * "Tendencia 30 días" widget (FOR-164 dashboard mockup): the recent weight /
 * body-fat / lean-mass trends from FOR-17 measurements, as three stacked charts.
 *
 * <p><b>Three charts and not three lines in one.</b> The card used to overlay
 * the metrics on a single plot, which meant reconciling kilos with percentage
 * points — and the way it reconciled them was to rescale every series against
 * its OWN minimum and maximum. That is not one axis, it is three invisible ones
 * drawn as if they were one, and it lies twice: 1,7 kg of weight and 1,8 points
 * of body fat get stretched to the same height, so scale noise draws the same
 * mountain as a real cut; and two lines that rise together share no scale, so
 * the crossings mean nothing. For measures in different units there are exactly
 * two honest fixes — one chart per series, or index them all to a common base.
 * This is the first.
 *
 * <p>It also settles something colour could not. `--color-accent` (weight) and
 * `--color-warning-graphic` (body fat) sit at ΔE 0,8 under deuteranopia, where
 * the threshold for telling two adjacent series apart is 8: overlaid, those two
 * lines were the same line for roughly one reader in twelve. Repainting was not
 * available — the weight/fat/muscle colours are the whole panel's convention
 * (the body tiles, the nutrition rings), and a metric that changes colour
 * between two cards on one screen teaches something false. Giving each metric
 * its own plot removes the collision instead of recolouring around it.
 *
 * <p>The window is thirty *days*, ending today — which is what the card has
 * always been titled. It used to be the last thirty *measurements* (a
 * `slice(0, 30)` over the newest-first list), so an account with hundreds of
 * rows saw ten months of history under a label promising one, with the actual
 * recent month crushed into the last few pixels.
 *
 * <p>The three rows share one x axis, pinned to the span the measurements
 * actually cover, and the single pair of date labels under them names that same
 * span. Pinning matters because a metric missing on some dates would otherwise
 * draw the same fortnight at a different width from its neighbours — stacked
 * charts only compare if they share a scale.
 *
 * <p>That span is the data's, not the window's. The card used to label the axis
 * with the window's own bounds while plotting whatever fell inside it, which
 * made the two disagree: a week of measurements was drawn edge to edge under
 * dates claiming a month. Both readings are now the data's, and the *window*
 * is what the title says — so an account with one week of history sees a
 * legible week, correctly dated, in a card that still promises thirty days.
 * With nothing to plot inside the window the card says so, instead of falling
 * back to older data the title does not cover (ADR-006 — no fabricated trend).
 *
 * <p>El histórico llega por props. Lo buscaba él mismo, y era la tercera petición idéntica
 * de la misma carga — ver {@link MeasurementsState}.
 */
type State = MeasurementsState;

const WINDOW_DAYS = 30;
const DAY_MS = 24 * 60 * 60 * 1000;

/** Two charts can't be compared if one of them is a single point. */
const MIN_POINTS = 2;

/*
 * Colour per metric, and the assignment is the nutrition rings': weight green,
 * body fat amber, muscle blue — the same three the body tiles above use, so a
 * metric keeps one colour wherever the panel draws it. Never the only carrier:
 * each row is named in text and each chart has its own `ariaLabel`.
 */
const METRICS = [
  {
    key: 'weight',
    label: 'Peso',
    unit: 'kg',
    color: 'var(--color-accent)',
    select: (m: BodyMeasurement) => m.weightKg,
  },
  {
    key: 'fat',
    label: 'Grasa',
    unit: '%',
    color: 'var(--color-warning-graphic)',
    select: (m: BodyMeasurement) => m.bodyFatPercentage,
  },
  {
    key: 'lean',
    label: 'Músculo',
    unit: 'kg',
    color: 'var(--color-info)',
    select: (m: BodyMeasurement) => m.leanMassKg,
  },
] as const;

function toPoints(
  measurements: readonly BodyMeasurement[],
  select: (m: BodyMeasurement) => number | undefined,
): ChartPoint[] {
  return measurements.flatMap((m) => {
    const value = select(m);
    return value === undefined
      ? []
      : [
          {
            t: Date.parse(m.measuredAt),
            y: value,
            dateLabel: formatTimestamp(Date.parse(m.measuredAt)),
          },
        ];
  });
}

function formatTimestamp(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

export function TrendWidget({ state }: { readonly state: MeasurementsState }) {
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

  const rows = METRICS.map((metric) => ({
    metric,
    points: toPoints(window, metric.select),
  })).filter((row) => row.points.length >= MIN_POINTS);

  if (rows.length === 0) {
    return (
      <p className={styles.empty} role="status">
        No hay mediciones en los últimos {WINDOW_DAYS} días. Sigue registrando tus mediciones para
        ver tu evolución.
      </p>
    );
  }

  // Un solo dominio para las tres filas: el que abarcan de verdad las mediciones.
  const stamps = rows.flatMap((row) => row.points.map((point) => point.t));
  const firstAt = Math.min(...stamps);
  const lastAt = Math.max(...stamps);

  return (
    <div className={styles.card}>
      <ul className={styles.metrics}>
        {rows.map(({ metric, points }) => {
          const first = points[0].y;
          const last = points[points.length - 1].y;
          return (
            <li key={metric.key} className={styles.metric}>
              <div className={styles.head}>
                <span className={styles.name}>
                  <span
                    className={styles.dot}
                    style={{ backgroundColor: metric.color }}
                    aria-hidden="true"
                  />
                  {metric.label}
                </span>
                <span className={styles.figures}>
                  <span className={styles.value}>
                    {fixed(last)} {metric.unit}
                  </span>
                  {/*
                   * La variación no lleva unidad: va en la del valor que tiene
                   * al lado, y escribir «%» detrás de la de grasa se leería
                   * como un porcentaje DE un porcentaje, que -1.8 puntos no es.
                   */}
                  <span className={styles.delta}>{formatChange(last - first)}</span>
                </span>
              </div>
              <LineChart
                variant="spark"
                points={points}
                color={metric.color}
                xDomain={[firstAt, lastAt]}
                formatValue={(value) => `${fixed(value)} ${metric.unit}`}
                ariaLabel={`${metric.label} en los últimos ${WINDOW_DAYS} días: de ${fixed(first)} ${metric.unit} a ${fixed(last)} ${metric.unit}. ${points.length} mediciones.`}
              />
            </li>
          );
        })}
      </ul>
      <div className={styles.axisRow} aria-hidden="true">
        <span className={styles.axis}>{formatTimestamp(firstAt)}</span>
        <span className={styles.axis}>{formatTimestamp(lastAt)}</span>
      </div>
    </div>
  );
}

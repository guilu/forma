import { useEffect, useState } from 'react';
import { BodyFigure } from '../components/BodyFigure';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ChartContainer } from '../components/ChartContainer';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { LineChart, type ChartPoint } from '../components/LineChart';
import { LoadingState } from '../components/LoadingState';
import { MeasurementForm } from '../components/MeasurementForm';
import { MetricCard } from '../components/MetricCard';
import { Modal } from '../components/Modal';
import { StatusPill } from '../components/StatusPill';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import styles from './MeasurementsPage.module.css';

/**
 * Measurements page (FOR-18, built out to the mockup by FOR-52):
 * `docs/2-mediciones.png` — latest metric cards with sparklines, a weight
 * evolution chart with a range selector, a recent-history table and the
 * manual-entry form, all reading from the FOR-17 body measurements API. No
 * value here is computed by the UI (ADR-001/ADR-006) — every number is read
 * straight from the API response.
 *
 * <p>Mockup fields not backed by the FOR-17 API today (documented gap, not
 * invented — AGENTS.md "repository state has priority"):
 * <ul>
 *   <li>"AGUA CORPORAL" (body-water %) — no `waterPercentage` field exists on
 *       `BodyMeasurement` yet (specs/FOR-15/spec.md explicitly defers it).
 *   <li>"DISTRIBUCIÓN CORPORAL" silhouette (Músculo/Grasa/Hueso/Agua) — same
 *       reason; the domain has no muscle/bone/water breakdown.
 *   <li>"vs semana pasada" deltas per card — `WeeklyBodySummary` (FOR-21)
 *       computes a comparable delta, but it is not exposed over HTTP (no
 *       field on `GET /api/v1/body/measurements`, and the FOR-45
 *       `/api/v1/insights/weekly` check-in snapshot explicitly omits deltas
 *       too — see `RecoveryWarningRules` comment "the check-in does not carry
 *       deltas"). Recomputing a "nearest prior measurement" comparison here
 *       would duplicate that domain rule in the UI, which FOR-51 already
 *       ruled out for the same dashboard data — this story keeps that
 *       decision.
 *   <li>The IMC "Saludable" category badge — BMI thresholds are not modeled
 *       anywhere in the domain/API; only the raw `bmi` number is returned.
 * </ul>
 * "Masa muscular" labels `leanMassKg` (lean mass, not a separate muscle-mass
 * figure — the domain has no `muscleMassKg` field), matching the label the
 * FOR-51 dashboard widget already uses for the same value.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly measurements: BodyMeasurement[] };

type TabKey = 'resumen' | 'evolucion' | 'historial';

const TABS: ReadonlyArray<{ key: TabKey; label: string }> = [
  { key: 'resumen', label: 'Resumen' },
  { key: 'evolucion', label: 'Evolución' },
  { key: 'historial', label: 'Historial' },
];

const SPARKLINE_WINDOW = 8;
const HISTORY_PREVIEW_ROWS = 5;
const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * FOR-164 hybrid placeholders (`docs/2-mediciones.png`). Body-water % and the
 * bone/water breakdown of the composition figure are NOT modeled on
 * `BodyMeasurement` (specs/FOR-15 defers body water; there is no bone field) —
 * shown as isolated, clearly-labelled template scaffolding so they're obvious
 * and easy to replace once those fields exist. Muscle (leanMassKg) and fat
 * (fatMassKg) in the figure are REAL, read straight from the measurement.
 */
const PLACEHOLDER = {
  waterPercentage: 58.0,
  boneKg: 3.2,
  waterKg: 42.7,
} as const;

interface MetricConfig {
  readonly key: string;
  readonly label: string;
  readonly unit?: string;
  readonly value: (m: BodyMeasurement) => number | undefined;
}

/** The four latest-metric cards the FOR-17 API actually supports (no water %). */
const METRICS: readonly MetricConfig[] = [
  { key: 'weight', label: 'Peso', unit: 'kg', value: (m) => m.weightKg },
  { key: 'fat', label: 'Grasa corporal', unit: '%', value: (m) => m.bodyFatPercentage },
  { key: 'lean', label: 'Masa muscular', unit: 'kg', value: (m) => m.leanMassKg },
  { key: 'bmi', label: 'IMC', value: (m) => m.bmi },
];

interface RangeOption {
  readonly key: string;
  readonly label: string;
  readonly days: number | null;
}

const RANGE_OPTIONS: readonly RangeOption[] = [
  { key: '7D', label: '7D', days: 7 },
  { key: '1M', label: '1M', days: 30 },
  { key: '3M', label: '3M', days: 90 },
  { key: '6M', label: '6M', days: 180 },
  { key: '1A', label: '1A', days: 365 },
  { key: 'ALL', label: 'Todo', days: null },
];

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

function formatValue(value: number | undefined, decimals = 1): string {
  return value === undefined ? '—' : value.toFixed(decimals);
}

/** Builds chronological (oldest → newest) chart points for one metric. */
function seriesFor(measurements: BodyMeasurement[], metric: MetricConfig): ChartPoint[] {
  return measurements
    .map((m) => ({ value: metric.value(m), measuredAt: m.measuredAt }))
    .filter((p): p is { value: number; measuredAt: string } => typeof p.value === 'number')
    .reverse() // measurements are newest-first; charts read oldest-first
    .map((p) => ({ t: Date.parse(p.measuredAt), y: p.value, dateLabel: formatDate(p.measuredAt) }));
}

/** Points within `days` of the latest point; the full series when `days` is null. */
function pointsInRange(points: ChartPoint[], days: number | null): ChartPoint[] {
  if (days === null || points.length === 0) {
    return points;
  }
  const latestT = points[points.length - 1].t;
  const cutoff = latestT - days * DAY_MS;
  return points.filter((p) => p.t >= cutoff);
}

/**
 * Only offers a range button when it actually narrows the view below the
 * full history (spec FOR-52: "cap chart range options to the data actually
 * available") — otherwise a button would show the exact same series as
 * "Todo", which isn't a meaningful choice. "Todo" is always offered.
 */
function availableRanges(points: ChartPoint[]): readonly RangeOption[] {
  return RANGE_OPTIONS.filter((option) => {
    if (option.days === null) {
      return true;
    }
    const filtered = pointsInRange(points, option.days);
    return filtered.length >= 2 && filtered.length < points.length;
  });
}

export function MeasurementsPage() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [formOpen, setFormOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<TabKey>('resumen');

  function load() {
    setState({ status: 'loading' });
    listBodyMeasurements()
      .then((measurements) => {
        setState(
          measurements.length === 0 ? { status: 'empty' } : { status: 'ready', measurements },
        );
      })
      .catch(() => {
        setState({ status: 'error' });
      });
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Mediciones</h1>
          <p className={styles.subtitle}>Controla tu composición corporal y evolución.</p>
        </div>
        <Button type="button" onClick={() => setFormOpen(true)}>
          + Registrar medición
        </Button>
      </header>

      {renderContent(state, activeTab, setActiveTab, () => setFormOpen(true), load)}

      {formOpen && (
        <Modal title="Registrar medición" onClose={() => setFormOpen(false)}>
          <MeasurementForm
            onCancel={() => setFormOpen(false)}
            onCreated={() => {
              setFormOpen(false);
              load();
            }}
          />
        </Modal>
      )}
    </div>
  );
}

function renderContent(
  state: State,
  activeTab: TabKey,
  setActiveTab: (tab: TabKey) => void,
  openForm: () => void,
  reload: () => void,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tus mediciones…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudieron cargar tus mediciones. Inténtalo de nuevo."
        onRetry={reload}
      />
    );
  }

  if (state.status === 'empty') {
    return (
      <EmptyState
        title="Aún no hay mediciones."
        action={
          <Button type="button" onClick={openForm}>
            Registrar medición
          </Button>
        }
      />
    );
  }

  return (
    <MeasurementsDashboard
      measurements={state.measurements}
      activeTab={activeTab}
      setActiveTab={setActiveTab}
    />
  );
}

interface DashboardProps {
  readonly measurements: BodyMeasurement[];
  readonly activeTab: TabKey;
  readonly setActiveTab: (tab: TabKey) => void;
}

function MeasurementsDashboard({ measurements, activeTab, setActiveTab }: DashboardProps) {
  const latest = measurements[0];

  return (
    <>
      <div className={styles.tabs} role="tablist" aria-label="Secciones de mediciones">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            id={`tab-${tab.key}`}
            aria-selected={activeTab === tab.key}
            aria-controls={`panel-${tab.key}`}
            className={styles.tab}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <section
        id="panel-resumen"
        role="tabpanel"
        aria-labelledby="tab-resumen"
        data-active={activeTab === 'resumen'}
        className={styles.panel}
      >
        <div className={styles.cardsGrid}>
          {METRICS.map((metric) => {
            const points = seriesFor(measurements, metric).slice(-SPARKLINE_WINDOW);
            return (
              <MetricCard
                key={metric.key}
                label={metric.label}
                headingLevel={2}
                value={formatValue(metric.value(latest))}
                unit={metric.unit}
                trend={
                  points.length >= 2 ? (
                    <LineChart
                      points={points}
                      formatValue={(v) => v.toFixed(1)}
                      ariaLabel={`Evolución reciente de ${metric.label.toLowerCase()}`}
                    />
                  ) : undefined
                }
              />
            );
          })}
          {/* Agua corporal — placeholder (no body-water field on the API yet,
              see PLACEHOLDER). No "vs semana pasada" delta (not backed). */}
          <MetricCard
            label="Agua corporal"
            headingLevel={2}
            value={PLACEHOLDER.waterPercentage.toFixed(1)}
            unit="%"
            caption="Estimación"
          />
        </div>
      </section>

      <section
        id="panel-evolucion"
        role="tabpanel"
        aria-labelledby="tab-evolucion"
        data-active={activeTab === 'evolucion'}
        className={styles.panel}
      >
        <WeightEvolutionChart measurements={measurements} />
      </section>

      <section
        id="panel-historial"
        role="tabpanel"
        aria-labelledby="tab-historial"
        data-active={activeTab === 'historial'}
        className={styles.panel}
      >
        <div className={styles.historyRow}>
          <HistoryTable measurements={measurements} />
          <BodyDistributionCard latest={latest} />
        </div>
      </section>
    </>
  );
}

/**
 * "Distribución corporal" card (FOR-164 mockup): a placeholder body figure plus
 * a composition legend. Músculo (`leanMassKg`) and Grasa (`fatMassKg`) are REAL
 * when the measurement carries them; Hueso and Agua are isolated placeholders
 * (no such fields on the API — see {@link PLACEHOLDER}). Swap {@link BodyFigure}
 * for the real asset pack later.
 */
function BodyDistributionCard({ latest }: { readonly latest: BodyMeasurement }) {
  const rows = [
    { key: 'muscle', label: 'Músculo', value: latest.leanMassKg, unit: 'kg', dot: styles.dotMuscle, real: true },
    { key: 'fat', label: 'Grasa', value: latest.fatMassKg, unit: 'kg', dot: styles.dotFat, real: true },
    { key: 'bone', label: 'Hueso', value: PLACEHOLDER.boneKg, unit: 'kg', dot: styles.dotBone, real: false },
    { key: 'water', label: 'Agua', value: PLACEHOLDER.waterKg, unit: 'kg', dot: styles.dotWater, real: false },
  ];

  return (
    <Card title="Distribución corporal" headingLevel={2}>
      <div className={styles.distribution}>
        <BodyFigure variant="strength" active size={168} label="Composición corporal" />
        <ul className={styles.distributionLegend}>
          {rows.map((row) => (
            <li key={row.key} className={styles.distributionItem}>
              <span className={`${styles.distributionDot} ${row.dot}`} aria-hidden="true" />
              <span className={styles.distributionLabel}>{row.label}</span>
              <span className={styles.distributionValue}>
                {row.value === undefined ? '—' : `${row.value.toFixed(1)} ${row.unit}`}
              </span>
            </li>
          ))}
        </ul>
      </div>
      <a className={styles.detailLink} href="/progreso">
        Ver análisis detallado
      </a>
    </Card>
  );
}

function WeightEvolutionChart({ measurements }: { readonly measurements: BodyMeasurement[] }) {
  const allPoints = seriesFor(measurements, METRICS[0]);
  const ranges = availableRanges(allPoints);
  const [selectedRange, setSelectedRange] = useState('ALL');
  const active = ranges.find((r) => r.key === selectedRange) ?? ranges[ranges.length - 1];
  const points = pointsInRange(allPoints, active.days);
  const latestPoint = points[points.length - 1];

  return (
    <ChartContainer
      title="Evolución de peso"
      headingLevel={2}
      state={points.length >= 2 ? 'ready' : 'empty'}
      emptyMessage="Necesitas al menos dos mediciones para ver la evolución."
      action={
        <div className={styles.rangeSelector} role="group" aria-label="Rango del gráfico">
          {ranges.map((range) => (
            <button
              key={range.key}
              type="button"
              className={styles.rangeButton}
              aria-pressed={range.key === active.key}
              onClick={() => setSelectedRange(range.key)}
            >
              {range.label}
            </button>
          ))}
        </div>
      }
    >
      {points.length >= 2 && (
        <>
          {latestPoint && (
            <p className={styles.latest}>
              {latestPoint.y.toFixed(1)} kg{' '}
              <span className={styles.latestDate}>· {latestPoint.dateLabel}</span>
            </p>
          )}
          <LineChart
            points={points}
            formatValue={(v) => `${v.toFixed(1)} kg`}
            ariaLabel={`Evolución de peso: ${points.length} mediciones, de ${points[0].y.toFixed(1)} kg a ${latestPoint.y.toFixed(1)} kg.`}
          />
        </>
      )}
    </ChartContainer>
  );
}

function HistoryTable({ measurements }: { readonly measurements: BodyMeasurement[] }) {
  const [expanded, setExpanded] = useState(false);
  const rows = expanded ? measurements : measurements.slice(0, HISTORY_PREVIEW_ROWS);

  return (
    <Card title="Últimas mediciones" headingLevel={2}>
      <div className={styles.tableScroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">Fecha</th>
              <th scope="col">Peso</th>
              <th scope="col">Grasa corporal</th>
              <th scope="col">Masa muscular</th>
              <th scope="col">IMC</th>
              <th scope="col">Fuente</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((m) => (
              <tr key={m.measuredAt}>
                <td>{formatDate(m.measuredAt)}</td>
                <td>{formatValue(m.weightKg)} kg</td>
                <td>{formatValue(m.bodyFatPercentage)} %</td>
                <td>{formatValue(m.leanMassKg)} kg</td>
                <td>{formatValue(m.bmi)}</td>
                <td>
                  <StatusPill kind="source" value={m.source || 'UNKNOWN'} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!expanded && measurements.length > HISTORY_PREVIEW_ROWS && (
        <Button variant="ghost" type="button" onClick={() => setExpanded(true)}>
          Ver todas las mediciones
        </Button>
      )}
    </Card>
  );
}

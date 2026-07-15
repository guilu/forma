/**
 * Display-only formatting helpers for {@link GoalsPage} (FOR-122): label/unit
 * lookups for the closed `GoalMetric`/`GoalStatus` enums (FOR-125) and date/
 * value formatters. Split out of the component file so this pure logic is
 * directly unit-testable and the component file keeps exporting only the
 * component (`react-refresh/only-export-components`), matching the sibling
 * `shoppingDisplay.ts`/`shoppingCategories.ts` convention (FOR-111/FOR-117).
 *
 * <p>Presentation-only mapping (label/unit text), never a business rule or a
 * progress computation (architecture-overview.md, ADR-006) — the numbers
 * themselves always come straight from the FOR-125 read model.
 */
import type { GoalMetric, GoalStatus } from '../api/goals';

const METRIC_LABELS: ReadonlyMap<GoalMetric, string> = new Map([
  ['BODY_FAT_PCT', 'Grasa corporal'],
  ['WEIGHT_KG', 'Peso corporal'],
  ['LEAN_MASS_KG', 'Masa magra'],
]);

const METRIC_UNITS: ReadonlyMap<GoalMetric, string> = new Map([
  ['BODY_FAT_PCT', '%'],
  ['WEIGHT_KG', 'kg'],
  ['LEAN_MASS_KG', 'kg'],
]);

const STATUS_LABELS: ReadonlyMap<GoalStatus, string> = new Map([
  ['ACTIVE', 'Activo'],
  ['ACHIEVED', 'Conseguido'],
  ['ARCHIVED', 'Archivado'],
]);

/** Display label for a `GoalMetric` value; falls back to the raw value if unrecognized. */
export function metricLabel(metric: string): string {
  return METRIC_LABELS.get(metric as GoalMetric) ?? metric;
}

/** Unit suffix for a `GoalMetric` value; empty string if unrecognized. */
export function metricUnit(metric: string): string {
  return METRIC_UNITS.get(metric as GoalMetric) ?? '';
}

/** Display label for a `GoalStatus` value; falls back to the raw value if unrecognized. */
export function statusLabel(status: string): string {
  return STATUS_LABELS.get(status as GoalStatus) ?? status;
}

const dueDateFormatter = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

const NO_DUE_DATE = 'Sin fecha límite';

/** Formats a goal's optional `dueDate` (ISO-8601), or a calm placeholder when absent. */
export function formatDueDate(dueDate: string | null | undefined): string {
  if (!dueDate) {
    return NO_DUE_DATE;
  }
  // Parsed as a plain date (no time component), matching the backend's
  // `LocalDate` — avoids a UTC/local timezone day-shift for date-only strings.
  return dueDateFormatter.format(new Date(`${dueDate}T00:00:00`));
}

const NO_VALUE = '—';

/**
 * Formats a metric value with its unit. `null` (unlinked/no-data metric,
 * FOR-125 api.md) renders {@link NO_VALUE} — never fabricated as `0`.
 */
export function formatMetricValue(value: number | null, metric: string): string {
  if (value == null) {
    return NO_VALUE;
  }
  const unit = metricUnit(metric);
  return unit ? `${value} ${unit}` : String(value);
}

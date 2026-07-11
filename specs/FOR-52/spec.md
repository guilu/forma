# FOR-52: Create body composition screens

Jira: https://dbhlab.atlassian.net/browse/FOR-52
Epic: FOR-47 UI & UX

## Summary

Build the body composition (Mediciones) screens: latest metrics, historical list,
trend chart, manual entry form, derived metrics and a manual-vs-imported source
indicator. Mockup: `docs/2-mediciones.png`. `MeasurementsPage.tsx` and
`MeasurementForm.tsx` already exist (FOR-18/19/20); this story aligns them to the
mockup and fills gaps. Must not present noisy smart-scale data as lab-grade.

## User/System Flow

1. User opens Mediciones (`/mediciones`).
2. Latest metric cards + weight evolution chart + last-measurements table load
   from the body read endpoints (FOR-17).
3. User opens "Registrar medición" → manual entry form → submit → list/cards
   refresh.

## Functional Requirements

- **Latest measurement cards**: PESO, GRASA CORPORAL, MASA MUSCULAR, IMC, AGUA
  CORPORAL with "vs semana pasada" deltas + sparklines (reuse `MetricCard`,
  `LineChart`).
- **Trend chart**: "EVOLUCIÓN DE PESO" with range selector (7D/1M/3M/6M/1A/Todo)
  and the latest-value callout.
- **Historical list**: "ÚLTIMAS MEDICIONES" table (Fecha, Peso, Grasa, Masa,
  IMC, Agua) + "Ver todas las mediciones".
- **Manual entry form**: reuse/extend `MeasurementForm`; field-level validation
  errors shown close to fields.
- **Derived metrics**: from the FOR-15 domain type (fat/lean mass) — never
  recomputed in UI.
- **Source indicator**: distinguish manual vs imported (Withings) measurements.
- Empty state before the first measurement; loading + error states (FOR-60).

## Non-Functional Requirements

- No fake precision (docs/ui-guidelines.md): show values as provided.
- Consumes read endpoints; no body calculations in UI (ADR-001).

## Data Model Notes

Consumes FOR-17 body measurements API (list + create). Derived values come from
the API/domain. **Mockup extras not yet backed**: body "DISTRIBUCIÓN CORPORAL"
silhouette (Músculo/Grasa/Hueso/Agua breakdown), "AGUA CORPORAL %" — render only
if the API provides them; otherwise placeholder/omit (repository priority).

## Edge Cases

- No measurements yet → empty state, entry CTA, no broken chart/table.
- One measurement → cards without deltas (nothing to compare).
- Import source unknown/missing → default to a neutral source label.
- Validation errors on entry → shown inline; list preserved.

## Open Questions

- Whether the body-distribution silhouette + water% are backed by FOR-17 today —
  audit; if not, defer/placeholder and document.
- Chart range options beyond what history supports — cap to available data.

import type { ReactNode } from 'react';
import { Card, type HeadingLevel } from './Card';
import styles from './MetricCard.module.css';

/**
 * Single dashboard metric tile (FOR-19, extended FOR-52 with an optional
 * `trend` slot). Reuses {@link Card} for the surface and its uppercase muted
 * label, and renders a large, readable numeric value with an optional unit —
 * matching the metric cards in docs/1-dashboard.png / docs/2-mediciones.png.
 * Purely presentational: it displays already-formatted values and never
 * computes anything (ADR-006).
 *
 * <p>`trend` is typically a small {@link LineChart} sparkline rendered below
 * the value (docs/2-mediciones.png shows one per card); omitted entirely when
 * there isn't enough history to plot, so callers control that decision.
 *
 * <p>`headingLevel` (FOR-112) forwards straight to {@link Card} so the label
 * can match the page's actual heading order; defaults to Card's own default
 * (`3`) when omitted.
 */
interface MetricCardProps {
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
  readonly trend?: ReactNode;
  readonly headingLevel?: HeadingLevel;
}

export function MetricCard({ label, value, unit, trend, headingLevel }: MetricCardProps) {
  return (
    <Card title={label} headingLevel={headingLevel}>
      <p className={styles.value}>
        {value}
        {unit && <span className={styles.unit}> {unit}</span>}
      </p>
      {trend && <div className={styles.trend}>{trend}</div>}
    </Card>
  );
}

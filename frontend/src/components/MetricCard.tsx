import type { ReactNode } from 'react';
import { Card, type HeadingLevel } from './Card';
import { Icon, type IconName } from './Icon';
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
  /**
   * Optional decorative icon rendered top-right of the tile (FOR-164 shopping
   * mockup: each summary tile carries a category glyph). Passed to {@link Card}
   * as its header `action` so it aligns with the muted uppercase label; purely
   * visual (`aria-hidden`, like every {@link Icon}).
   */
  readonly icon?: IconName;
}

export function MetricCard({ label, value, unit, trend, headingLevel, icon }: MetricCardProps) {
  return (
    <Card
      title={label}
      headingLevel={headingLevel}
      action={icon && <Icon name={icon} className={styles.icon} size={20} />}
    >
      <p className={styles.value}>
        {value}
        {unit && <span className={styles.unit}> {unit}</span>}
      </p>
      {trend && <div className={styles.trend}>{trend}</div>}
    </Card>
  );
}

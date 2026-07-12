import type { ReactNode } from 'react';
import { Icon } from './Icon';
import styles from './EmptyState.module.css';

/**
 * Shared empty-state component (FOR-60): standardizes the "no data yet"
 * messages every feature page rendered ad hoc (`MeasurementsPage`,
 * `NutritionPage`, `BodyWidget`, …) into one calm, actionable layout —
 * message + optional primary action (spec `specs/FOR-60/ui.md`: "calm,
 * actionable 'no data yet' with an optional primary action").
 *
 * <p>Two variants, both `role="status"` since "no data" is not a failure:
 * <ul>
 *   <li>`feature` (default) — nothing has been created yet for this feature
 *       (e.g. "Aún no hay mediciones."). Shows the inbox icon and is meant to
 *       anchor a page/section, optionally with a primary-action `Button`
 *       (e.g. "Registrar medición").
 *   <li>`filtered` — data exists but the current filter/search/tab narrowed
 *       it to nothing (e.g. "No hay artículos que coincidan con el filtro
 *       actual."). Deliberately more compact and icon-less so it reads as a
 *       transient view state, not a first-run empty feature — this is what
 *       keeps the two empty states visually/semantically distinct (edge case
 *       in `specs/FOR-60/spec.md`).
 * </ul>
 */
export type EmptyStateVariant = 'feature' | 'filtered';

interface EmptyStateProps {
  readonly variant?: EmptyStateVariant;
  readonly title: string;
  readonly description?: string;
  readonly action?: ReactNode;
}

export function EmptyState({ variant = 'feature', title, description, action }: EmptyStateProps) {
  const wrapperClassName =
    variant === 'filtered' ? `${styles.wrapper} ${styles.filtered}` : styles.wrapper;

  return (
    <div className={wrapperClassName} role="status">
      {variant === 'feature' && <Icon name="inbox" size={28} className={styles.icon} />}
      <p className={styles.title}>{title}</p>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}

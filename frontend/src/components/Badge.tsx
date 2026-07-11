import type { ReactNode } from 'react';
import styles from './Badge.module.css';

export type BadgeTone = 'accent' | 'warning' | 'danger' | 'neutral';

/**
 * Small status/label pill (FOR-50). The base primitive behind severity,
 * connection and plazo tags (see {@link StatusPill}) and ad-hoc labels like
 * "Saludable". `neutral` is the safe default so an unrecognized value never
 * renders unstyled (spec FOR-50 edge case).
 *
 * <p>Outlined (colored border + text, no solid fill) to match the existing
 * inline-message convention in `MeasurementForm.module.css` (`.formError` /
 * `.success`) and to stay restrained per docs/ui-guidelines.md.
 */
interface BadgeProps {
  readonly tone?: BadgeTone;
  readonly children: ReactNode;
  readonly className?: string;
}

export function Badge({ tone = 'neutral', children, className }: BadgeProps) {
  return (
    <span className={[styles.badge, className].filter(Boolean).join(' ')} data-tone={tone}>
      {children}
    </span>
  );
}

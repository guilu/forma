import type { ReactNode } from 'react';
import styles from './ProgressRing.module.css';

/**
 * Generic progress ring (FOR-164 dashboard mockup: the "CALORÍAS HOY 78%" and
 * "TU PRIMER RESUMEN 1 de 1" donuts). Renders an already-computed
 * `value`/`max` fraction as an accent conic-gradient arc with a punched-through
 * center that holds arbitrary `children` (a percentage, a count, etc.).
 *
 * <p>Purely presentational (ADR-006 — like {@link ProgressBar}/{@link MacroRing}):
 * it clamps and rounds for display only and never derives the numbers itself.
 * The `label` is the accessible summary so the value survives without the
 * visual (ui.md accessibility).
 */
interface ProgressRingProps {
  readonly value: number;
  readonly max: number;
  readonly label: string;
  readonly children?: ReactNode;
  /** Ring diameter in px (default 96, matching MacroRing). */
  readonly size?: number;
}

export function ProgressRing({ value, max, label, children, size = 96 }: ProgressRingProps) {
  const percent = max > 0 ? Math.min(100, Math.max(0, Math.round((value / max) * 100))) : 0;
  const ringStyle = {
    width: size,
    height: size,
    background: `conic-gradient(var(--color-accent) ${percent * 3.6}deg, var(--color-border) ${
      percent * 3.6
    }deg 360deg)`,
  };

  return (
    <div className={styles.ring} style={ringStyle} role="img" aria-label={label}>
      <div className={styles.hole}>{children}</div>
    </div>
  );
}

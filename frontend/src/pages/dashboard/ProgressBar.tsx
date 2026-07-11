import styles from './ProgressBar.module.css';

/**
 * Minimal horizontal progress indicator (FOR-51), in the spirit of `LineChart`
 * (ADR-010 — no chart library, plain inline styling). Renders a already-computed
 * fraction as a filled bar; it never derives the fraction itself (callers pass
 * numbers straight from a read model). The percentage is also rendered as text so
 * the value is available without relying on the visual bar (ui.md accessibility:
 * "metrics are text, screen-reader friendly").
 */
interface ProgressBarProps {
  readonly value: number;
  readonly max: number;
  readonly label: string;
}

export function ProgressBar({ value, max, label }: ProgressBarProps) {
  const percent = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className={styles.wrapper}>
      <div
        className={styles.track}
        role="progressbar"
        aria-label={label}
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className={styles.fill} style={{ width: `${percent}%` }} />
      </div>
      <span className={styles.text}>{percent}%</span>
    </div>
  );
}

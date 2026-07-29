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
  /**
   * Fill colour, any CSS colour (e.g. a `var(--…)`). Defaults to the accent.
   * Set it where several bars sit together and each stands for a different
   * thing (the dashboard's macros) — never as the only carrier of that
   * distinction, which is why every bar is also labelled in text.
   */
  readonly color?: string;
  /** Hides the trailing "%" text where the caller prints its own figures. */
  readonly showPercent?: boolean;
}

export function ProgressBar({ value, max, label, color, showPercent = true }: ProgressBarProps) {
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
        <div className={styles.fill} style={{ width: `${percent}%`, backgroundColor: color }} />
      </div>
      {showPercent && <span className={styles.text}>{percent}%</span>}
    </div>
  );
}

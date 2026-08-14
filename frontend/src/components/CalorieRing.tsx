import styles from './CalorieRing.module.css';

/**
 * Calories eaten against the day's target, as a ring.
 *
 * <p>Not {@link MacroRing}, which answers a different question: that one splits a whole into three
 * shares, this one shows how far along one number is towards another. Sharing a component between
 * them would mean a ring whose sweep means two things.
 *
 * <p>The sweep is capped at a full turn while the figures below are not: going over the target is
 * something to see, and a ring that kept winding past twelve o'clock would read as less than it is.
 */
interface CalorieRingProps {
  readonly consumed: number;
  /** The day's target, or `null` when the plan sets none — then there is no ring to draw. */
  readonly target: number | null;
  readonly compact?: boolean;
}

const NUM = new Intl.NumberFormat('es-ES');

export function CalorieRing({ consumed, target, compact = false }: CalorieRingProps) {
  const ratio = target && target > 0 ? Math.min(consumed / target, 1) : 0;
  const remaining = target === null ? null : Math.max(target - consumed, 0);

  return (
    <div className={compact ? `${styles.wrapper} ${styles.compact}` : styles.wrapper}>
      <div
        className={styles.ring}
        style={{ '--sweep': `${ratio * 360}deg` } as React.CSSProperties}
        role="img"
        aria-label={
          target === null
            ? `${NUM.format(consumed)} kcal consumidas. Tu plan no fija un objetivo.`
            : `${NUM.format(consumed)} de ${NUM.format(target)} kcal consumidas.`
        }
      >
        <p className={styles.figures}>
          <span className={styles.consumed}>{NUM.format(consumed)}</span>
          {target !== null && <span className={styles.target}>/ {NUM.format(target)} kcal</span>}
        </p>
      </div>
      {!compact && (
        <dl className={styles.legend}>
          <div>
            <dt>Consumidas</dt>
            <dd>{NUM.format(consumed)}</dd>
          </div>
          <div>
            <dt>Restantes</dt>
            {/* Un guion y no un cero: nadie ha fijado objetivo, que no es lo mismo que no quedar nada. */}
            <dd>{remaining === null ? '—' : NUM.format(remaining)}</dd>
          </div>
        </dl>
      )}
    </div>
  );
}

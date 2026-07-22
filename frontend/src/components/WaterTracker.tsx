import { Card, type HeadingLevel } from './Card';
import styles from './WaterTracker.module.css';

/**
 * Hydration tracker tile (FOR-164 dashboard mockup: "AGUA 2.1 / 2.5 L · 84%"
 * with a row of glasses).
 *
 * <p><b>Placeholder data.</b> There is no hydration endpoint anywhere in the
 * backend (verified: no controller, no read model, no persisted intake), so
 * this widget renders static template values rather than live data — the same
 * honest "static placeholder" precedent as {@link SyncWidget}'s Withings chip.
 * The numbers are visual scaffolding for the mockup, NOT a real measurement,
 * and must be replaced once a hydration API exists. Kept in one clearly-named
 * constant so the fake data is obvious and easy to rip out.
 */
const PLACEHOLDER = {
  currentL: 2.1,
  goalL: 2.5,
  glassesTotal: 5,
  glassesFilled: 4,
} as const;

const NUM = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

export function WaterTracker({ headingLevel }: { readonly headingLevel?: HeadingLevel } = {}) {
  const { currentL, goalL, glassesTotal, glassesFilled } = PLACEHOLDER;
  const percent = Math.round((currentL / goalL) * 100);

  return (
    <Card title="Agua" headingLevel={headingLevel}>
      <p className={styles.value}>
        {NUM.format(currentL)}
        <span className={styles.goal}> / {NUM.format(goalL)} L</span>
      </p>
      <p className={styles.percent}>{percent}%</p>
      <div
        className={styles.glasses}
        role="img"
        aria-label={`Hidratación: ${NUM.format(currentL)} de ${NUM.format(goalL)} litros (${percent}%)`}
      >
        {Array.from({ length: glassesTotal }, (_, i) => (
          <span
            key={i}
            className={i < glassesFilled ? styles.glassFilled : styles.glass}
            aria-hidden="true"
          />
        ))}
      </div>
    </Card>
  );
}

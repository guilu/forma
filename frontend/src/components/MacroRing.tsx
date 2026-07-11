import styles from './MacroRing.module.css';

/**
 * Macro-distribution ring (FOR-54). Renders the three macro targets
 * (Proteínas/Carbohidratos/Grasas, in grams) as a decorative conic-gradient
 * ring plus a text legend, matching `docs/4-nutricion.png`'s "DISTRIBUCIÓN DE
 * MACROS" panel.
 *
 * <p>Proportions are gram-based, not calorie-based: converting grams to kcal
 * uses the 4/4/9 kcal-per-gram factors, which is domain logic owned by the
 * backend {@code NutritionCalculator} (ADR-001 — no nutrition calculations in
 * the UI). The mockup's percentages read as calorie shares; this ring shows a
 * gram-share approximation instead so the UI never repeats that conversion
 * itself (documented gap, FOR-54 PR "Known limitations"). All three numbers
 * are also rendered as plain text with units, so the information is available
 * without relying on the visual (ui.md accessibility: "macro values are text
 * with units; ring has an accessible summary").
 */
interface MacroRingProps {
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

const LEGEND = [
  { key: 'protein', label: 'Proteínas', className: 'protein' },
  { key: 'carbs', label: 'Carbohidratos', className: 'carbs' },
  { key: 'fat', label: 'Grasas', className: 'fat' },
] as const;

export function MacroRing({ proteinG, carbsG, fatG }: MacroRingProps) {
  const total = proteinG + carbsG + fatG;
  const proteinDeg = total > 0 ? (proteinG / total) * 360 : 0;
  const carbsDeg = total > 0 ? (carbsG / total) * 360 : 0;
  const values = { protein: proteinG, carbs: carbsG, fat: fatG };

  const ringStyle = {
    background: `conic-gradient(var(--color-accent) 0deg ${proteinDeg}deg, var(--color-warning) ${proteinDeg}deg ${
      proteinDeg + carbsDeg
    }deg, var(--color-text-muted) ${proteinDeg + carbsDeg}deg 360deg)`,
  };

  return (
    <div className={styles.wrapper}>
      <div
        className={styles.ring}
        style={ringStyle}
        role="img"
        aria-label={`Objetivo de macronutrientes: proteínas ${proteinG} gramos, carbohidratos ${carbsG} gramos, grasas ${fatG} gramos`}
      >
        <div className={styles.hole} aria-hidden="true" />
      </div>
      <ul className={styles.legend}>
        {LEGEND.map((entry) => (
          <li key={entry.key} className={styles.legendItem}>
            <span className={`${styles.dot} ${styles[entry.className]}`} aria-hidden="true" />
            <span className={styles.legendLabel}>{entry.label}</span>
            <span className={styles.legendValue}>{values[entry.key]} g</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

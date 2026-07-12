import type { OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Nutrition basics step (FOR-59 FR: "minimal preferences"). Local draft
 * answer only — no nutrition-preferences backend exists yet (bootstrap).
 * Copy stays preference-framed, never diagnostic (AGENTS.md: no medical/
 * diagnosis language) — "restricciones" asks about personal food choices,
 * not medical conditions.
 */
interface NutritionBasicsStepProps {
  readonly value: OnboardingAnswers['nutrition'];
  readonly onChange: (patch: Partial<OnboardingAnswers['nutrition']>) => void;
}

export function NutritionBasicsStep({ value, onChange }: NutritionBasicsStepProps) {
  return (
    <div className={styles.field}>
      <p className={styles.intro}>Cuéntanos un poco sobre tu alimentación (opcional).</p>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-nutrition-preference">
          Preferencia alimentaria
        </label>
        <select
          id="onboarding-nutrition-preference"
          className={styles.select}
          value={value.preference}
          onChange={(event) => onChange({ preference: event.target.value })}
        >
          <option value="">Prefiero no decirlo</option>
          <option value="OMNIVORE">Omnívora</option>
          <option value="VEGETARIAN">Vegetariana</option>
          <option value="VEGAN">Vegana</option>
          <option value="GLUTEN_FREE">Sin gluten</option>
          <option value="OTHER">Otra</option>
        </select>
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-nutrition-restrictions">
          Alimentos que prefieres evitar (opcional)
        </label>
        <textarea
          id="onboarding-nutrition-restrictions"
          className={styles.textarea}
          rows={3}
          value={value.restrictions}
          onChange={(event) => onChange({ restrictions: event.target.value })}
        />
      </div>
    </div>
  );
}

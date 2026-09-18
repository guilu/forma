import type { OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Nutrition basics step (FOR-59 FR: "minimal preferences"), redesigned by
 * ADR-015 slice 3.
 *
 * <p><b>The free-text "restrictions" question is gone (ADR-015 decision
 * 12).</b> It asked "Alimentos que prefieres evitar" in a plain textarea,
 * and people type diagnoses into free-text boxes — the repository's own test
 * fixture used to fill it with {@code 'Frutos secos'}, a nut allergy in
 * every practical sense. That is GDPR article 9 special-category data with
 * no reader and no purpose (nothing in this codebase ever consumed it), so
 * the question is removed rather than left dormant. {@code
 * onboarding_nutrition_preference} (below) survives: an enum of dietary
 * patterns is a preference, not a health record.
 *
 * <p><b>Two new questions, local-only.</b> "Estilo de cocina" and "Comidas
 * al día" feed {@code PlanRequestCreateRequest.cuisineStyle}/{@code
 * .mealsPerDay} at the wizard's final submission (see {@code
 * planRequestMapping.ts}) — the backend requires both, and the wizard did
 * not ask either before this slice. They are never synced through the
 * draft-progress {@code PATCH /api/v1/profile/onboarding} call (see {@link
 * import('../onboardingStorage').toOnboardingAnswersInput}), whose backend
 * contract has no field for them.
 */
const MEALS_PER_DAY_OPTIONS = [3, 4, 5, 6] as const;

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
        <label className={styles.label} htmlFor="onboarding-nutrition-cuisine">
          Estilo de cocina
        </label>
        <select
          id="onboarding-nutrition-cuisine"
          className={styles.select}
          value={value.cuisineStyle}
          onChange={(event) => onChange({ cuisineStyle: event.target.value })}
        >
          <option value="">Prefiero no decirlo</option>
          <option value="ESPANOLA">Española</option>
          <option value="MEDITERRANEA">Mediterránea</option>
        </select>
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-nutrition-meals">
          Comidas al día
        </label>
        <select
          id="onboarding-nutrition-meals"
          className={styles.select}
          value={String(value.mealsPerDay)}
          onChange={(event) => onChange({ mealsPerDay: Number(event.target.value) })}
        >
          {MEALS_PER_DAY_OPTIONS.map((meals) => (
            <option key={meals} value={meals}>
              {meals}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}

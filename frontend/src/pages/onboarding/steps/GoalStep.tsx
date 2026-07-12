import type { GoalOption, OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Goal selection step (FOR-59 FR: "choose a main objective (composición/
 * rendimiento/hábito), aligned with the Objetivos concept"). Mirrors
 * `docs/7-objetivos.png` conceptually — the mockup's "Objetivos" screen
 * itself has no dedicated backend/story (see `specs/FOR-58` Open Questions),
 * so this only *captures* a selection into the local onboarding draft; it is
 * never persisted as a real goal (documented gap, tracked for a future
 * goals-backend story).
 */
const GOAL_OPTIONS: ReadonlyArray<{ id: GoalOption; title: string; description: string }> = [
  {
    id: 'COMPOSICION',
    title: 'Composición corporal',
    description: 'Cambiar tu proporción de grasa y masa muscular.',
  },
  {
    id: 'RENDIMIENTO',
    title: 'Rendimiento',
    description: 'Mejorar tu capacidad física y tus marcas.',
  },
  {
    id: 'HABITO',
    title: 'Hábito',
    description: 'Ser constante con el entrenamiento y la nutrición.',
  },
];

interface GoalStepProps {
  readonly value: OnboardingAnswers['goal'];
  readonly onChange: (patch: Partial<OnboardingAnswers['goal']>) => void;
}

export function GoalStep({ value, onChange }: GoalStepProps) {
  return (
    <div className={styles.field}>
      <p className={styles.intro}>
        Elige el objetivo principal que quieres trabajar ahora. Podrás ajustarlo más adelante.
      </p>
      <div className={styles.choiceGrid} role="radiogroup" aria-label="Objetivo principal">
        {GOAL_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            role="radio"
            aria-checked={value.selected === option.id}
            className={styles.choiceCard}
            onClick={() => onChange({ selected: option.id })}
          >
            <span className={styles.choiceTitle}>{option.title}</span>
            <span className={styles.choiceDescription}>{option.description}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

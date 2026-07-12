import type { OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Training availability step (FOR-59 FR: "capture days/equipment for
 * plans"). No training-plan backend consumes this yet (bootstrap) — it is a
 * local draft answer like the rest of onboarding, captured for a future
 * plan-generation story.
 */
const DAYS: readonly string[] = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
];

interface TrainingAvailabilityStepProps {
  readonly value: OnboardingAnswers['training'];
  readonly onChange: (patch: Partial<OnboardingAnswers['training']>) => void;
}

export function TrainingAvailabilityStep({ value, onChange }: TrainingAvailabilityStepProps) {
  function toggleDay(day: string) {
    const days = value.days.includes(day)
      ? value.days.filter((d) => d !== day)
      : [...value.days, day];
    onChange({ days });
  }

  return (
    <div className={styles.field}>
      <p className={styles.intro}>¿Qué días de la semana puedes entrenar? (opcional)</p>
      <div
        className={styles.checkboxGroup}
        role="group"
        aria-label="Días disponibles para entrenar"
      >
        {DAYS.map((day) => (
          <label key={day} className={styles.checkboxOption}>
            <input
              type="checkbox"
              checked={value.days.includes(day)}
              onChange={() => toggleDay(day)}
            />
            {day}
          </label>
        ))}
      </div>
    </div>
  );
}

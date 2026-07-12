import type { OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Profile confirmation step (FOR-59 FR: "basic personal fields"). The only
 * critical (non-skippable) step in the flow — everything downstream assumes
 * at least a name, so onboarding treats this as the minimum viable identity
 * check rather than a full profile edit (that belongs to FOR-58, which is
 * display-only today, no profile backend — ADR-002).
 *
 * <p>Only "Nombre" is required; birth date, sex and height are optional
 * context that improve future guidance but must not block a new user from
 * reaching the rest of onboarding (spec: "calm copy", no forced precision).
 */
interface ProfileStepProps {
  readonly value: OnboardingAnswers['profile'];
  readonly onChange: (patch: Partial<OnboardingAnswers['profile']>) => void;
  readonly error?: string;
}

export function ProfileStep({ value, onChange, error }: ProfileStepProps) {
  return (
    <div className={styles.field}>
      <p className={styles.intro}>
        Confirma algunos datos básicos para personalizar tu experiencia en FORMA.
      </p>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-name">
          Nombre
        </label>
        <input
          id="onboarding-name"
          className={styles.input}
          type="text"
          value={value.name}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'onboarding-name-error' : undefined}
          onChange={(event) => onChange({ name: event.target.value })}
        />
        {error && (
          <p id="onboarding-name-error" className={styles.fieldError}>
            {error}
          </p>
        )}
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-birthdate">
          Fecha de nacimiento (opcional)
        </label>
        <input
          id="onboarding-birthdate"
          className={styles.input}
          type="date"
          value={value.birthDate}
          onChange={(event) => onChange({ birthDate: event.target.value })}
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-sex">
          Sexo (opcional)
        </label>
        <select
          id="onboarding-sex"
          className={styles.select}
          value={value.sex}
          onChange={(event) => onChange({ sex: event.target.value })}
        >
          <option value="">Prefiero no decirlo</option>
          <option value="FEMALE">Mujer</option>
          <option value="MALE">Hombre</option>
          <option value="OTHER">Otro</option>
        </select>
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="onboarding-height">
          Altura en cm (opcional)
        </label>
        <input
          id="onboarding-height"
          className={styles.input}
          type="number"
          min="0"
          value={value.heightCm}
          onChange={(event) => onChange({ heightCm: event.target.value })}
        />
      </div>
    </div>
  );
}

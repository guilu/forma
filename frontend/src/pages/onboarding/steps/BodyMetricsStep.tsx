import { Link } from 'react-router-dom';
import { MeasurementForm } from '../../../components/MeasurementForm';
import type { BodyMetricsChoice, OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Body metrics step (FOR-59 FR: "manual entry (reuse MeasurementForm, FOR-52)
 * or import prompt (FOR-57)"). Reuses {@link MeasurementForm} verbatim — it
 * already posts to the real FOR-17 API, so a measurement saved here is a real,
 * persisted record, unlike the rest of onboarding's local-only draft answers.
 *
 * <p>The "import" choice does not re-embed the full FOR-57
 * `IntegrationsSection` (that widget is reused wholesale by the dedicated
 * final "Conectar integración" step instead) — it is a lightweight prompt
 * pointing at that entry point, matching the spec's own wording ("import
 * prompt"), to avoid rendering two full integration widgets in one flow.
 */
interface BodyMetricsStepProps {
  readonly value: OnboardingAnswers['metrics'];
  readonly onChange: (patch: Partial<OnboardingAnswers['metrics']>) => void;
}

export function BodyMetricsStep({ value, onChange }: BodyMetricsStepProps) {
  function selectChoice(choice: BodyMetricsChoice) {
    onChange({ choice });
  }

  function handleCreated() {
    onChange({ measurementSaved: true });
  }

  return (
    <div className={styles.field}>
      <p className={styles.intro}>
        Registra tu punto de partida. Puedes introducirlo a mano o importarlo más adelante desde
        Withings.
      </p>

      <div className={styles.choiceGrid} role="group" aria-label="Cómo quieres añadir tus métricas">
        <button
          type="button"
          className={styles.choiceCard}
          aria-pressed={value.choice === 'MANUAL'}
          onClick={() => selectChoice('MANUAL')}
        >
          <span className={styles.choiceTitle}>Registrar ahora</span>
          <span className={styles.choiceDescription}>Introduce tu peso y medidas actuales.</span>
        </button>
        <button
          type="button"
          className={styles.choiceCard}
          aria-pressed={value.choice === 'IMPORT'}
          onClick={() => selectChoice('IMPORT')}
        >
          <span className={styles.choiceTitle}>Importar más tarde</span>
          <span className={styles.choiceDescription}>
            Conecta Withings desde Ajustes para sincronizar tus datos automáticamente.
          </span>
        </button>
      </div>

      {value.choice === 'MANUAL' && (
        <>
          {value.measurementSaved ? (
            <p className={styles.savedNotice} role="status">
              Medición guardada correctamente.
            </p>
          ) : (
            <MeasurementForm onCreated={handleCreated} />
          )}
        </>
      )}

      {value.choice === 'IMPORT' && (
        <div className={styles.field}>
          <p className={styles.hint}>
            Puedes conectar Withings ahora o más tarde desde Ajustes → Integraciones.
          </p>
          <Link className={styles.linkButton} to="/ajustes/integraciones">
            Ir a Integraciones
          </Link>
        </div>
      )}
    </div>
  );
}

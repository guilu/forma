import { useEffect, useRef } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import styles from './CompletionStep.module.css';

/**
 * Completion screen (FOR-59 `ui.md`: "Completion screen with a clear next
 * action"). Shown once every onboarding step has been passed through, and
 * also reused as the "already completed" gate when a returning user opens
 * `/onboarding` again (spec edge case: resume must never trap or re-block a
 * user who already finished — a manual local flag is enough for the MVP,
 * see `onboardingStorage.ts`).
 *
 * <p>No confetti/gamified language (docs/ui-guidelines.md interaction
 * style) — a calm confirmation and one clear next action.
 */
interface CompletionStepProps {
  readonly alreadyCompleted: boolean;
  readonly onGoToDashboard: () => void;
  readonly onRestart: () => void;
}

export function CompletionStep({
  alreadyCompleted,
  onGoToDashboard,
  onRestart,
}: CompletionStepProps) {
  const headingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  return (
    <div className={styles.wrapper}>
      <Card>
        <h2 ref={headingRef} tabIndex={-1} className={styles.title}>
          {alreadyCompleted ? 'Ya completaste la configuración inicial' : 'Todo listo'}
        </h2>
        <p className={styles.description}>
          {alreadyCompleted
            ? 'Puedes volver al panel cuando quieras, o repetir la configuración inicial si quieres cambiar tus respuestas.'
            : 'Hemos guardado tus preferencias iniciales. Podrás ajustarlas en cualquier momento desde Ajustes.'}
        </p>
        <div className={styles.actions}>
          <Button variant="primary" type="button" onClick={onGoToDashboard}>
            Ir al panel
          </Button>
          {alreadyCompleted && (
            <Button variant="secondary" type="button" onClick={onRestart}>
              Volver a empezar
            </Button>
          )}
        </div>
      </Card>
    </div>
  );
}

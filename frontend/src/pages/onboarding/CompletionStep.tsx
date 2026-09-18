import { useEffect, useRef } from 'react';
import { Button } from '../../components/Button';
import { ButtonLink } from '../../components/ButtonLink';
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

/**
 * Feedback from the wizard's final, additional submission — `POST
 * /api/v1/plan-requests` (ADR-015 slice 3) — fired once, when the flow is
 * genuinely finished. `tone` picks the accessible role: `'status'` for
 * information that does not block anything (a plan was requested, or one
 * was already open), `'alert'` for something the person still has to act on
 * before a plan can be requested (a missing direction, incomplete profile
 * data). This screen never blocks "Ir al panel" on the outcome — slice 7
 * (the waiting/result screens) owns what happens after the request exists;
 * this only has to report honestly that it was sent, or explain why it
 * was not.
 *
 * <p>The action is one of two kinds, per `docs/ui-guidelines.md`'s
 * `ButtonLink` rule ("a `<button>` that calls `navigate()` throws away
 * middle-click, open-in-new-tab and the browser's own link handling"):
 * `'link'` for a real navigation away from this page (to Ajustes or
 * Mediciones, for profile data this wizard never collects), rendered as a
 * {@link ButtonLink}; `'step'` for jumping back to an earlier step of this
 * same wizard (no URL change), rendered as a plain {@link Button}.
 */
export interface PlanRequestNotice {
  readonly message: string;
  readonly tone: 'status' | 'alert';
  readonly action?:
    | { readonly kind: 'link'; readonly label: string; readonly to: string }
    | { readonly kind: 'step'; readonly label: string; readonly onClick: () => void };
}

interface CompletionStepProps {
  readonly alreadyCompleted: boolean;
  readonly onGoToDashboard: () => void;
  readonly onRestart: () => void;
  readonly planRequestNotice?: PlanRequestNotice;
}

export function CompletionStep({
  alreadyCompleted,
  onGoToDashboard,
  onRestart,
  planRequestNotice,
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
        {planRequestNotice && (
          <p
            role={planRequestNotice.tone === 'alert' ? 'alert' : 'status'}
            className={styles.planRequestNotice}
          >
            {planRequestNotice.message}
          </p>
        )}
        {planRequestNotice?.action?.kind === 'link' && (
          <ButtonLink variant="secondary" to={planRequestNotice.action.to}>
            {planRequestNotice.action.label}
          </ButtonLink>
        )}
        {planRequestNotice?.action?.kind === 'step' && (
          <Button variant="secondary" type="button" onClick={planRequestNotice.action.onClick}>
            {planRequestNotice.action.label}
          </Button>
        )}
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

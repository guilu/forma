import { useEffect, useRef } from 'react';
import { Button } from '../../components/Button';
import { ProgressBar } from '../dashboard/ProgressBar';
import styles from './OnboardingStepShell.module.css';

/**
 * Step wrapper (FOR-59 `ui.md` Components: "progress indicator, title,
 * content, back/next/skip"). Every onboarding step renders through this
 * shell so navigation, progress and error placement stay identical across
 * steps — steps themselves only ever provide field content.
 *
 * <p>Accessibility (`ui.md`): the step heading receives focus on every step
 * change so screen-reader users land on the new step instead of wherever
 * focus happened to be, and progress is announced both visually
 * ({@link ProgressBar}, reused from FOR-51) and as plain text ("Paso X de N").
 */
interface OnboardingStepShellProps {
  readonly stepIndex: number;
  readonly totalSteps: number;
  readonly title: string;
  readonly error?: string;
  readonly canGoBack: boolean;
  readonly skippable: boolean;
  readonly nextLabel?: string;
  readonly onBack: () => void;
  readonly onNext: () => void;
  readonly onSkip: () => void;
  readonly children: React.ReactNode;
}

export function OnboardingStepShell({
  stepIndex,
  totalSteps,
  title,
  error,
  canGoBack,
  skippable,
  nextLabel = 'Siguiente',
  onBack,
  onNext,
  onSkip,
  children,
}: OnboardingStepShellProps) {
  const headingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    headingRef.current?.focus();
  }, [stepIndex]);

  const stepLabel = `Paso ${stepIndex + 1} de ${totalSteps}`;

  return (
    <div className={styles.wrapper}>
      <div className={styles.progressRow}>
        <ProgressBar value={stepIndex + 1} max={totalSteps} label={stepLabel} />
        <p className={styles.stepCounter} role="status">
          {stepLabel}
        </p>
      </div>

      <h2 ref={headingRef} tabIndex={-1} className={styles.heading}>
        {title}
      </h2>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <div className={styles.content}>{children}</div>

      <div className={styles.actions}>
        <Button variant="secondary" type="button" onClick={onBack} disabled={!canGoBack}>
          Atrás
        </Button>
        <div className={styles.actionsTrailing}>
          {skippable && (
            <Button variant="ghost" type="button" onClick={onSkip}>
              Omitir este paso
            </Button>
          )}
          <Button variant="primary" type="button" onClick={onNext}>
            {nextLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

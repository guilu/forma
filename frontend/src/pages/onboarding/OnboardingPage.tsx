import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Brand } from '../../components/Brand';
import { useNotify } from '../../components/NotificationProvider';
import { OnboardingStepShell } from './OnboardingStepShell';
import { CompletionStep } from './CompletionStep';
import { ProfileStep } from './steps/ProfileStep';
import { BodyMetricsStep } from './steps/BodyMetricsStep';
import { GoalStep } from './steps/GoalStep';
import { TrainingAvailabilityStep } from './steps/TrainingAvailabilityStep';
import { EquipmentStep } from './steps/EquipmentStep';
import { NutritionBasicsStep } from './steps/NutritionBasicsStep';
import { IntegrationStep } from './steps/IntegrationStep';
import {
  clearOnboardingProgress,
  fetchOnboardingBackendState,
  hasOnboardingProgress,
  loadOnboardingProgress,
  saveOnboardingProgress,
  syncOnboardingProgress,
  INITIAL_PROGRESS,
  type OnboardingAnswers,
  type OnboardingProgress,
} from './onboardingStorage';
import styles from './OnboardingPage.module.css';

const SYNC_FAILED_MESSAGE =
  'No se pudieron guardar tus respuestas en el servidor. Se han guardado localmente y lo intentaremos más tarde.';

/**
 * First-run onboarding flow (FOR-59). A multi-step flow — profile
 * confirmation, current body metrics, goal selection, training
 * availability, equipment, nutrition basics and an integration prompt —
 * ending with a clear next action back to the dashboard.
 *
 * <p><b>Route placement</b>: mounted at `/onboarding` as a sibling of the
 * `AppShell` route tree (see `app/routes.tsx`), not inside it. Onboarding is
 * not a navigation section — it is intentionally absent from
 * `app/navigation.ts` — and rendering it outside `AppShell` keeps a mid-flow
 * user from being one sidebar click away from abandoning it (spec `ui.md`:
 * "new route/overlay").
 *
 * <p><b>First-run detection (FOR-121)</b>: the "already completed" gate now
 * also reads the backend's `firstRunCompleted` flag ({@link
 * fetchOnboardingBackendState}, FOR-107), merged with the local
 * `localStorage` flag ({@link loadOnboardingProgress}) as `local ||
 * backend === true` — the backend can only *confirm* completion (recovering
 * a cleared/never-written local flag) and never *revoke* a local one, so a
 * slow/stale/failed backend read can never kick a returning user who already
 * finished back into onboarding. The local flag also drives the very first
 * paint, so there is no flash of the wrong screen while the fetch resolves,
 * and it is the fallback if the backend is unreachable — a fetch failure
 * must never trap a user in or out of onboarding either way. This story
 * deliberately does not add a forced redirect from `/` into `/onboarding` —
 * `AGENTS.md`/story guidance is explicit that a manual route is enough for
 * the MVP, and a destructive automatic redirect could trap a returning user
 * with no way out; that decision is unchanged, only where the "already
 * completed" signal comes from. Once the user interacts (advances/restarts),
 * the backend gate stops being consulted for the rest of the session so a
 * late-resolving fetch never yanks the screen out from under an in-progress
 * or just-restarted flow.
 *
 * <p><b>Persistence (FOR-121)</b>: every answer is written to the local
 * draft (`onboardingStorage.ts`) immediately (fast, synchronous, never
 * loses in-progress input) and synced to the backend in the background at
 * each step boundary ({@link syncOnboardingProgress}) — fire-and-forget, so
 * a slow/failed backend call never blocks navigation; a failed sync shows a
 * non-blocking `useNotify` warning instead. The body-metrics step is
 * unaffected — it still reuses `MeasurementForm` (via {@link
 * BodyMetricsStep}) to write a real measurement through the FOR-17 API,
 * independent of this draft sync.
 */
type StepId =
  'profile' | 'metrics' | 'goal' | 'training' | 'equipment' | 'nutrition' | 'integration';

const STEP_ORDER: readonly StepId[] = [
  'profile',
  'metrics',
  'goal',
  'training',
  'equipment',
  'nutrition',
  'integration',
];

const STEP_TITLES: Record<StepId, string> = {
  profile: 'Perfil',
  metrics: 'Métricas actuales',
  goal: 'Objetivo',
  training: 'Disponibilidad de entrenamiento',
  equipment: 'Equipamiento',
  nutrition: 'Preferencias de nutrición',
  integration: 'Conectar integración',
};

/**
 * Profile is the only critical (non-skippable) step — every other step is
 * optional context that improves future guidance but must not block a new
 * user from reaching the dashboard (spec: "skip for non-critical steps").
 */
const SKIPPABLE: Record<StepId, boolean> = {
  profile: false,
  metrics: true,
  goal: true,
  training: true,
  equipment: true,
  nutrition: true,
  integration: true,
};

/**
 * Only two steps enforce real validation: the required name (profile) and
 * requiring an explicit goal choice when advancing via "Siguiente" (skip
 * still bypasses it, matching the spec's own distinction between "blocked
 * advance" and "skip past a non-critical step").
 */
function validateStep(id: StepId, answers: OnboardingAnswers): string | undefined {
  if (id === 'profile') {
    return answers.profile.name.trim().length > 0
      ? undefined
      : 'Introduce tu nombre para continuar.';
  }
  if (id === 'goal') {
    return answers.goal.selected ? undefined : 'Selecciona un objetivo o pulsa "Omitir este paso".';
  }
  return undefined;
}

function renderStepContent(
  id: StepId,
  answers: OnboardingAnswers,
  updateSection: <K extends keyof OnboardingAnswers>(
    key: K,
    patch: Partial<OnboardingAnswers[K]>,
  ) => void,
  error: string | undefined,
): ReactNode {
  switch (id) {
    case 'profile':
      return (
        <ProfileStep
          value={answers.profile}
          onChange={(patch) => updateSection('profile', patch)}
          error={error}
        />
      );
    case 'metrics':
      return (
        <BodyMetricsStep
          value={answers.metrics}
          onChange={(patch) => updateSection('metrics', patch)}
        />
      );
    case 'goal':
      return <GoalStep value={answers.goal} onChange={(patch) => updateSection('goal', patch)} />;
    case 'training':
      return (
        <TrainingAvailabilityStep
          value={answers.training}
          onChange={(patch) => updateSection('training', patch)}
        />
      );
    case 'equipment':
      return (
        <EquipmentStep
          value={answers.equipment}
          onChange={(patch) => updateSection('equipment', patch)}
        />
      );
    case 'nutrition':
      return (
        <NutritionBasicsStep
          value={answers.nutrition}
          onChange={(patch) => updateSection('nutrition', patch)}
        />
      );
    case 'integration':
      return <IntegrationStep />;
  }
}

export function OnboardingPage() {
  const navigate = useNavigate();
  const notify = useNotify();
  const [progress, setProgress] = useState<OnboardingProgress>(() => loadOnboardingProgress());
  const [error, setError] = useState<string | undefined>(undefined);
  // Backend-sourced first-run gate (FOR-121): `undefined` means "not resolved
  // yet, or the fetch failed" — the render falls back to the local flag in
  // that case, per the graceful-fallback requirement (never trap the user in
  // or out of onboarding on a fetch failure).
  const [backendCompleted, setBackendCompleted] = useState<boolean | undefined>(undefined);
  // Once the user drives the flow themselves (advances a step or restarts),
  // a late-resolving backend fetch must stop overriding the gate — otherwise
  // it could yank the screen mid-interaction or undo an explicit restart.
  const interactedRef = useRef(false);

  useEffect(() => {
    saveOnboardingProgress(progress);
  }, [progress]);

  useEffect(() => {
    let active = true;
    fetchOnboardingBackendState().then((state) => {
      if (!active || !state || interactedRef.current) {
        return;
      }
      setBackendCompleted(state.firstRunCompleted);
      // Recovery (spec edge case): localStorage was cleared/never written but
      // the backend already has prior progress — recover it rather than
      // restarting blind. Only applies while the local draft is still empty.
      setProgress((prev) => {
        if (hasOnboardingProgress(prev.answers) || !hasOnboardingProgress(state.answers)) {
          return prev;
        }
        return { ...prev, answers: state.answers };
      });
    });
    return () => {
      active = false;
    };
  }, []);

  const totalSteps = STEP_ORDER.length;
  const atEnd = progress.stepIndex >= totalSteps;
  // Backend can only *confirm* completion (recovering a cleared/never-written
  // local flag, spec edge case), never revoke a local one — a backend read
  // that hasn't caught up yet (or a past sync failure) must not kick a
  // returning user who already finished back into onboarding.
  const completed = progress.completed || backendCompleted === true;

  function updateSection<K extends keyof OnboardingAnswers>(
    key: K,
    patch: Partial<OnboardingAnswers[K]>,
  ) {
    setProgress((prev) => ({
      ...prev,
      answers: { ...prev.answers, [key]: { ...prev.answers[key], ...patch } },
    }));
  }

  /** Background sync (FOR-121): never awaited by callers, never blocks navigation. */
  function syncInBackground(next: OnboardingProgress) {
    void syncOnboardingProgress(next).then((ok) => {
      if (!ok) {
        notify.warning(SYNC_FAILED_MESSAGE);
      }
    });
  }

  function goToStep(index: number) {
    interactedRef.current = true;
    setError(undefined);
    const next: OnboardingProgress = { ...progress, stepIndex: index };
    setProgress(next);
    syncInBackground(next);
  }

  function handleNext() {
    const currentId = STEP_ORDER[progress.stepIndex];
    const validationError = validateStep(currentId, progress.answers);
    if (validationError) {
      setError(validationError);
      return;
    }
    goToStep(progress.stepIndex + 1);
  }

  function handleSkip() {
    goToStep(progress.stepIndex + 1);
  }

  function handleBack() {
    goToStep(Math.max(0, progress.stepIndex - 1));
  }

  function handleGoToDashboard() {
    interactedRef.current = true;
    const next: OnboardingProgress = { ...progress, completed: true };
    setProgress(next);
    syncInBackground(next);
    navigate('/');
  }

  function handleRestart() {
    interactedRef.current = true;
    setBackendCompleted(undefined);
    clearOnboardingProgress();
    setProgress(INITIAL_PROGRESS);
    setError(undefined);
  }

  if (completed || atEnd) {
    return (
      <div className={styles.page}>
        <OnboardingHeader />
        <div className={styles.panel}>
          <CompletionStep
            alreadyCompleted={completed}
            onGoToDashboard={handleGoToDashboard}
            onRestart={handleRestart}
          />
        </div>
      </div>
    );
  }

  const currentId = STEP_ORDER[progress.stepIndex];

  return (
    <div className={styles.page}>
      <OnboardingHeader />
      <div className={styles.panel}>
        <OnboardingStepShell
          stepIndex={progress.stepIndex}
          totalSteps={totalSteps}
          title={STEP_TITLES[currentId]}
          error={error}
          canGoBack={progress.stepIndex > 0}
          skippable={SKIPPABLE[currentId]}
          nextLabel={progress.stepIndex === totalSteps - 1 ? 'Finalizar' : 'Siguiente'}
          onBack={handleBack}
          onNext={handleNext}
          onSkip={handleSkip}
        >
          {renderStepContent(currentId, progress.answers, updateSection, error)}
        </OnboardingStepShell>
      </div>
    </div>
  );
}

/**
 * FOR-113: page-level heading for `/onboarding`, matching the
 * `<h1 className={styles.title}>` pattern every other page already uses
 * (`DashboardPage`, `MeasurementsPage`, `TrainingPage`, `NutritionPage`,
 * `ShoppingPage`, `ProgressPage`, `SettingsPage`, `IntegrationsPage`) — this
 * was the one page missing it (WCAG 2.4.6). Rendered once, outside the
 * per-step content, so it stays identical across every step instead of
 * resetting alongside {@link OnboardingStepShell}'s step `<h2>` (which keeps
 * receiving focus on step change, unaffected by this addition — FOR-61).
 */
function OnboardingHeader() {
  return (
    <div className={styles.header}>
      <Brand />
      <h1 className={styles.title}>Configuración inicial</h1>
      <p className={styles.subtitle}>Configuremos tu experiencia en unos pocos pasos.</p>
    </div>
  );
}

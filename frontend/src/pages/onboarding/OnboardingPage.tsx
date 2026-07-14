import { useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Brand } from '../../components/Brand';
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
  loadOnboardingProgress,
  saveOnboardingProgress,
  INITIAL_PROGRESS,
  type OnboardingAnswers,
  type OnboardingProgress,
} from './onboardingStorage';
import styles from './OnboardingPage.module.css';

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
 * <p><b>First-run detection</b>: there is no auth/user/profile backend yet
 * (ADR-002, single-user MVP), so there is nothing server-side to key a
 * redirect on. This story deliberately does not force-redirect existing
 * users from `/` into `/onboarding` — `AGENTS.md`/story guidance is explicit
 * that a manual route plus a local flag is enough for the MVP, and a
 * destructive automatic redirect could trap a returning user with no way
 * out. `/onboarding` is reachable directly; completion is tracked with a
 * `localStorage` flag ({@link loadOnboardingProgress}) purely so a returning
 * visitor to this URL sees "already completed" instead of restarting blind.
 * Wiring an automatic first-run redirect is future work once a real
 * session/user story exists (see PR "Backend gaps").
 *
 * <p><b>Persistence</b>: every answer is a local draft (`onboardingStorage.ts`)
 * designed for future persistence — except the body-metrics step, which
 * reuses `MeasurementForm` (via {@link BodyMetricsStep}) to write a real
 * measurement through the existing FOR-17 API. Goals, training availability,
 * equipment and nutrition preferences have no owning backend yet (documented
 * gap, tracked for a future enabler story).
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
  const [progress, setProgress] = useState<OnboardingProgress>(() => loadOnboardingProgress());
  const [error, setError] = useState<string | undefined>(undefined);

  useEffect(() => {
    saveOnboardingProgress(progress);
  }, [progress]);

  const totalSteps = STEP_ORDER.length;
  const atEnd = progress.stepIndex >= totalSteps;

  function updateSection<K extends keyof OnboardingAnswers>(
    key: K,
    patch: Partial<OnboardingAnswers[K]>,
  ) {
    setProgress((prev) => ({
      ...prev,
      answers: { ...prev.answers, [key]: { ...prev.answers[key], ...patch } },
    }));
  }

  function goToStep(index: number) {
    setError(undefined);
    setProgress((prev) => ({ ...prev, stepIndex: index }));
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
    setProgress((prev) => ({ ...prev, completed: true }));
    navigate('/');
  }

  function handleRestart() {
    clearOnboardingProgress();
    setProgress(INITIAL_PROGRESS);
    setError(undefined);
  }

  if (progress.completed || atEnd) {
    return (
      <div className={styles.page}>
        <OnboardingHeader />
        <div className={styles.panel}>
          <CompletionStep
            alreadyCompleted={progress.completed}
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

import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Brand } from '../../components/Brand';
import { Button } from '../../components/Button';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { createPlanRequest, getCurrentPlanRequest } from '../../api/planRequests';
import { OnboardingStepShell } from './OnboardingStepShell';
import { CompletionStep, type PlanRequestNotice } from './CompletionStep';
import { ProfileStep } from './steps/ProfileStep';
import { BodyMetricsStep } from './steps/BodyMetricsStep';
import { GoalStep } from './steps/GoalStep';
import { DirectionStep } from './steps/DirectionStep';
import { TrainingAvailabilityStep } from './steps/TrainingAvailabilityStep';
import { EquipmentStep } from './steps/EquipmentStep';
import { NutritionBasicsStep } from './steps/NutritionBasicsStep';
import { IntegrationStep } from './steps/IntegrationStep';
import { buildPlanRequestInput } from './planRequestMapping';
import { formatShortDate } from '../dateLabel';
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
 * "Nobody chose a direction" (ADR-015 decision 5's amendment): the one input
 * `buildPlanRequestInput` cannot default, so the wizard's own completion
 * never sends the request with a guessed value — it tells the person what
 * to fix instead.
 */
const MISSING_DIRECTION_MESSAGE =
  'No elegiste una dirección para tu plan (perder grasa, ganar músculo o mantenerte), así que no hemos podido pedirlo todavía.';

const PLAN_REQUEST_SENT_MESSAGE = 'Hemos enviado tu petición de un plan generado por IA.';

const PLAN_REQUEST_GENERIC_FAILURE_MESSAGE = 'No hemos podido enviar tu petición de plan.';

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
 * must never trap a user in or out of onboarding either way. Once the user
 * interacts (advances/restarts), the backend gate stops being consulted for
 * the rest of the session so a late-resolving fetch never yanks the screen
 * out from under an in-progress or just-restarted flow.
 *
 * <p><b>Forced redirect on first login (feat/onboarding-primera-vez,
 * supersedes the earlier "no forced redirect" decision this comment used to
 * document)</b>: a user who has not completed the first run is now sent to
 * `/onboarding` automatically — see {@link OnboardingGate}, mounted in
 * `AppShell` (`app/OnboardingGate.tsx`), not here. The criterion is this same
 * `firstRunCompleted` flag, not whether a plan exists, so a user who
 * finished onboarding but has no plan yet is never sent back — that empty
 * state belongs to the dashboard, not to this wizard. This page stays
 * unaware of the redirect: it only has to keep offering a way out, which it
 * already did for a returning user via {@link handleGoToDashboard} ("Ir al
 * panel" on the completion screen) and now also offers from every step via
 * the header's "Ahora no, ir al panel" exit ({@link OnboardingHeader}) — both
 * mark {@code completed: true} and persist it before navigating to `/app`,
 * so the redirect gate never sees them again. A forced entry that cannot be
 * dismissed would be a trap, not a guide; this flow was already built so
 * that it never is one.
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
  | 'profile'
  | 'metrics'
  | 'goal'
  | 'direction'
  | 'training'
  | 'equipment'
  | 'nutrition'
  | 'integration';

/**
 * `direction` sits right after `goal`, not folded into it (ADR-015 decision
 * 5's amendment). The two are adjacent questions that answer different
 * things — `goal` is the profile's standing life goal (`MainGoal`, stored on
 * `user_profile.main_goal`/its onboarding draft, unchanged by this slice);
 * `direction` is what THIS plan should do with calories
 * (`plan_request.plan_objective`, via `PlanDirection`). Placing them next to
 * each other keeps the two-question relationship visible without merging
 * them into one screen, which would blur which answer sets which field — a
 * `HABITO` user asking to `GAIN_MUSCLE` is an ordinary combination the
 * backend accepts, not a contradiction a shared screen should imply.
 */
const STEP_ORDER: readonly StepId[] = [
  'profile',
  'metrics',
  'goal',
  'direction',
  'training',
  'equipment',
  'nutrition',
  'integration',
];

const STEP_TITLES: Record<StepId, string> = {
  profile: 'Perfil',
  metrics: 'Métricas actuales',
  goal: 'Objetivo',
  direction: 'Dirección del plan',
  training: 'Disponibilidad de entrenamiento',
  equipment: 'Equipamiento',
  nutrition: 'Preferencias de nutrición',
  integration: 'Conectar integración',
};

/**
 * Profile is the only critical (non-skippable) step — every other step is
 * optional context that improves future guidance but must not block a new
 * user from reaching the dashboard (spec: "skip for non-critical steps").
 * `direction` follows the same rule as `goal`: skippable here, but a missing
 * answer is caught before the plan-request is ever sent (see {@link
 * buildAndSubmitPlanRequest}) rather than forcing a choice this early.
 */
const SKIPPABLE: Record<StepId, boolean> = {
  profile: false,
  metrics: true,
  goal: true,
  direction: true,
  training: true,
  equipment: true,
  nutrition: true,
  integration: true,
};

/**
 * Only three steps enforce real validation: the required name (profile) and
 * requiring an explicit goal/direction choice when advancing via "Siguiente"
 * (skip still bypasses it, matching the spec's own distinction between
 * "blocked advance" and "skip past a non-critical step").
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
  if (id === 'direction') {
    return answers.direction.selected
      ? undefined
      : 'Selecciona una dirección o pulsa "Omitir este paso".';
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
    case 'direction':
      return (
        <DirectionStep
          value={answers.direction}
          onChange={(patch) => updateSection('direction', patch)}
        />
      );
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
  // The wizard's final, additional submission (ADR-015 slice 3) — see
  // buildAndSubmitPlanRequest. undefined until the flow is finished once.
  const [planRequestNotice, setPlanRequestNotice] = useState<PlanRequestNotice | undefined>(
    undefined,
  );
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
    // Fires exactly on the transition into the completion screen via a real
    // "Finalizar"/"Omitir" click — never on a page reload that happens to
    // land back on an already-atEnd, not-yet-completed state, and never on
    // the "Ahora no, ir al panel" early exit (which calls
    // handleGoToDashboard, not goToStep). Re-fires correctly if the person
    // goes back (e.g. via the missing-direction notice's action) and
    // finishes the wizard again — see the dedicated OnboardingPage test.
    if (index >= totalSteps && !progress.completed) {
      setPlanRequestNotice(undefined);
      void buildAndSubmitPlanRequest(next.answers);
    }
  }

  /**
   * The wizard's final, additional submission (ADR-015 decision 8) — `POST
   * /api/v1/plan-requests`, distinct from {@link syncInBackground}'s
   * draft-progress PATCH, which keeps running exactly as it did before this
   * slice. Never blocks "Ir al panel": every branch below only ever sets
   * {@link planRequestNotice}, which {@link CompletionStep} renders
   * alongside its own actions, not in place of them.
   */
  async function buildAndSubmitPlanRequest(answers: OnboardingAnswers) {
    const input = buildPlanRequestInput(answers);
    if (!input) {
      setPlanRequestNotice({
        message: MISSING_DIRECTION_MESSAGE,
        tone: 'alert',
        action: {
          kind: 'step',
          label: 'Elegir dirección',
          onClick: () => goToStep(STEP_ORDER.indexOf('direction')),
        },
      });
      return;
    }

    try {
      await createPlanRequest(input);
      setPlanRequestNotice({ message: PLAN_REQUEST_SENT_MESSAGE, tone: 'status' });
    } catch (submitError) {
      await handlePlanRequestFailure(submitError);
    }
  }

  /**
   * Turns a failed `POST /api/v1/plan-requests` into an honest, specific
   * notice (ADR-015 slice 3 "handle the failures honestly") rather than a
   * raw error or a generic "algo ha ido mal":
   *
   * <ul>
   *   <li>409 CONFLICT — a request is already open; {@link
   *       getCurrentPlanRequest} explains it with real data instead of the
   *       raw error.
   *   <li>400 VALIDATION_ERROR — the caller's profile/body-measurement data
   *       is incomplete. The backend's own {@code message} already names
   *       which piece is missing ({@code PlanRequestService}); this only
   *       adds where to fix it, classified by the one fact the message
   *       reliably carries — which noun it names — since no field-level
   *       error code exists for this particular failure family.
   *   <li>anything else — the backend's safe {@code ApiError.message}
   *       (ADR-005: never a stack trace) shown as-is, which is already more
   *       specific than a generic fallback.
   * </ul>
   */
  async function handlePlanRequestFailure(submitError: unknown) {
    if (submitError instanceof ApiRequestError && submitError.status === 409) {
      try {
        const current = await getCurrentPlanRequest();
        setPlanRequestNotice({
          message: `Ya tienes una petición de plan en curso, solicitada el ${formatShortDate(
            new Date(current.requestedAt),
          )}.`,
          tone: 'status',
        });
      } catch {
        setPlanRequestNotice({ message: submitError.message, tone: 'status' });
      }
      return;
    }

    if (submitError instanceof ApiRequestError && submitError.status === 400) {
      setPlanRequestNotice({
        message: submitError.message,
        tone: 'alert',
        action: buildValidationAction(submitError.message),
      });
      return;
    }

    setPlanRequestNotice({
      message:
        submitError instanceof ApiRequestError
          ? submitError.message
          : PLAN_REQUEST_GENERIC_FAILURE_MESSAGE,
      tone: 'alert',
    });
  }

  /**
   * Classifies a `PlanRequestService` validation message to point at where
   * it can be fixed. There is no field-level error code for this family
   * (unlike bean-validation's `details[].field`) — `PlanRequestService`
   * throws a single-message `ValidationException` — so this reads the one
   * noun each message reliably names: "medición corporal" only ever means
   * the body-measurement step, "objetivo" only ever means the goal
   * question, and everything else in that service's messages ("tu sexo",
   * "tu altura", "tu nivel de actividad", "tu fecha de nacimiento") is a
   * canonical profile field this wizard does not itself collect (`activityLevel`
   * has no onboarding step at all) — so it routes to Ajustes.
   */
  function buildValidationAction(message: string): NonNullable<PlanRequestNotice['action']> {
    if (message.includes('medición corporal')) {
      return { kind: 'link', label: 'Registrar medición', to: '/app/measurements' };
    }
    if (message.includes('objetivo')) {
      return {
        kind: 'step',
        label: 'Elegir objetivo',
        onClick: () => goToStep(STEP_ORDER.indexOf('goal')),
      };
    }
    return { kind: 'link', label: 'Ir a Ajustes', to: '/app/settings' };
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
    navigate('/app');
  }

  function handleRestart() {
    interactedRef.current = true;
    setBackendCompleted(undefined);
    clearOnboardingProgress();
    setProgress(INITIAL_PROGRESS);
    setError(undefined);
    setPlanRequestNotice(undefined);
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
            planRequestNotice={planRequestNotice}
          />
        </div>
      </div>
    );
  }

  const currentId = STEP_ORDER[progress.stepIndex];

  return (
    <div className={styles.page}>
      <OnboardingHeader onExit={handleGoToDashboard} />
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
 *
 * <p><b>`onExit` (feat/onboarding-primera-vez)</b>: the explicit, visible way
 * out of a forced first-run redirect ({@link OnboardingGate}). Passed only on
 * the step view — the completion screen already offers the same exit as its
 * primary "Ir al panel" action, so a second copy here would be redundant.
 * Ghost-variant per `docs/ui-guidelines.md` (lowest emphasis, since the
 * primary action on every step is still "Siguiente"/"Finalizar").
 */
function OnboardingHeader({ onExit }: { readonly onExit?: () => void }) {
  return (
    <div className={styles.header}>
      <Brand />
      <h1 className={styles.title}>Configuración inicial</h1>
      <p className={styles.subtitle}>Configuremos tu experiencia en unos pocos pasos.</p>
      {onExit && (
        <Button variant="ghost" type="button" onClick={onExit}>
          Ahora no, ir al panel
        </Button>
      )}
    </div>
  );
}

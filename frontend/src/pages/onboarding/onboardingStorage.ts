/**
 * Local persistence for the first-run onboarding flow (FOR-59).
 *
 * <p><b>No onboarding/profile/goals backend exists yet</b> (verified: no
 * controller under `backend/src/main/java/.../delivery/**` for onboarding,
 * profile or goals; ADR-002 single-user MVP, AGENTS.md bootstrap status).
 * Per spec `specs/FOR-59/spec.md` Data Model Notes ("Steps that lack a
 * backend must be designed for future persistence — store progress
 * locally/in-memory and document"), this module is the single place that
 * reads/writes onboarding progress. It is deliberately isolated behind a
 * small read/write API (not scattered `localStorage.*` calls across step
 * components) so swapping it for a real `PATCH /api/v1/onboarding` call
 * later touches one file, not every step.
 *
 * <p>Answers are intentionally shallow, per-step buckets (not a normalized
 * domain model) — onboarding data has no owning aggregate yet (goals have no
 * backend at all, per FOR-58's same documented gap), so this stays a plain
 * UI-local draft, never presented as authoritative saved data outside the
 * body-metrics step (which does hit the real FOR-17 API via
 * {@link MeasurementForm}).
 */

export type GoalOption = 'COMPOSICION' | 'RENDIMIENTO' | 'HABITO';

export type BodyMetricsChoice = 'MANUAL' | 'IMPORT';

export interface OnboardingAnswers {
  readonly profile: {
    readonly name: string;
    readonly birthDate: string;
    readonly sex: string;
    readonly heightCm: string;
  };
  readonly metrics: {
    readonly choice: BodyMetricsChoice | undefined;
    readonly measurementSaved: boolean;
  };
  readonly goal: {
    readonly selected: GoalOption | undefined;
  };
  readonly training: {
    readonly days: readonly string[];
  };
  readonly equipment: {
    readonly items: readonly string[];
  };
  readonly nutrition: {
    readonly preference: string;
    readonly restrictions: string;
  };
}

export interface OnboardingProgress {
  readonly stepIndex: number;
  readonly answers: OnboardingAnswers;
  readonly completed: boolean;
}

/** Bumped if the stored shape ever changes incompatibly (defensive default fallback). */
const STORAGE_KEY = 'forma.onboarding.v1';

export const EMPTY_ANSWERS: OnboardingAnswers = {
  profile: { name: '', birthDate: '', sex: '', heightCm: '' },
  metrics: { choice: undefined, measurementSaved: false },
  goal: { selected: undefined },
  training: { days: [] },
  equipment: { items: [] },
  nutrition: { preference: '', restrictions: '' },
};

export const INITIAL_PROGRESS: OnboardingProgress = {
  stepIndex: 0,
  answers: EMPTY_ANSWERS,
  completed: false,
};

/**
 * Reads saved progress, tolerating missing/corrupted storage by falling back
 * to {@link INITIAL_PROGRESS}. Shallow-merges each answers bucket over the
 * defaults so a partially-written or older-shaped record never crashes the
 * flow — a missing field just resets to empty rather than throwing.
 */
export function loadOnboardingProgress(): OnboardingProgress {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return INITIAL_PROGRESS;
    }
    const parsed = JSON.parse(raw) as Partial<OnboardingProgress>;
    return {
      stepIndex: typeof parsed.stepIndex === 'number' ? parsed.stepIndex : 0,
      completed: parsed.completed === true,
      answers: {
        profile: { ...EMPTY_ANSWERS.profile, ...parsed.answers?.profile },
        metrics: { ...EMPTY_ANSWERS.metrics, ...parsed.answers?.metrics },
        goal: { ...EMPTY_ANSWERS.goal, ...parsed.answers?.goal },
        training: { ...EMPTY_ANSWERS.training, ...parsed.answers?.training },
        equipment: { ...EMPTY_ANSWERS.equipment, ...parsed.answers?.equipment },
        nutrition: { ...EMPTY_ANSWERS.nutrition, ...parsed.answers?.nutrition },
      },
    };
  } catch {
    // Storage unavailable (private browsing) or corrupted payload — start fresh
    // rather than breaking the flow (spec edge case: never block onboarding).
    return INITIAL_PROGRESS;
  }
}

/** Best-effort write; a storage failure must never break the onboarding flow. */
export function saveOnboardingProgress(progress: OnboardingProgress): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
  } catch {
    // Ignore — onboarding still works in-memory for the current session.
  }
}

/** Used by the "start over" action on the already-completed gate. */
export function clearOnboardingProgress(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore — nothing to clean up if storage was never usable.
  }
}

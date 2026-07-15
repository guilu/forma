/**
 * Local persistence for the first-run onboarding flow (FOR-59), extended by
 * FOR-121 to also sync with the backend.
 *
 * <p><b>FOR-121 — the anticipated swap</b>: this module was deliberately
 * isolated behind a small read/write API (not scattered `localStorage.*`
 * calls across step components) specifically so swapping it for a real
 * `PATCH /api/v1/profile/onboarding` call would touch one file, not every
 * step (FOR-107 shipped that endpoint). `localStorage` remains the fast
 * local draft/cache during the flow (a mid-flow reload never loses
 * not-yet-submitted answers, and a storage failure never breaks the flow);
 * the backend is now the authoritative record. Per-step sync (not
 * completion-only) was chosen, per the story's own recommendation, for
 * resilience against losing a long partial flow — {@link
 * syncOnboardingProgress} is called at every step boundary (Next/Skip/Back),
 * fire-and-forget from the caller's side so a slow/failed backend call never
 * blocks navigation (`OnboardingPage.tsx` shows a non-blocking `useNotify`
 * toast on failure instead).
 *
 * <p>Answers are intentionally shallow, per-step buckets (not a normalized
 * domain model) — mirrors the backend's own {@code OnboardingAnswers}
 * (verified field-for-field), which is deliberately shallow too. Never
 * presented as authoritative saved data outside the body-metrics step
 * (which hits the real FOR-17 API via {@link MeasurementForm}) and the
 * backend sync added here.
 */
import {
  getProfile,
  submitOnboardingAnswers,
  type OnboardingAnswersInput,
  type OnboardingAnswersOutput,
} from '../../api/profile';
import { apiClient, type ApiClient } from '../../api/client';

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

/** True once any answer group holds a real (non-blank/non-empty) value. */
export function hasOnboardingProgress(answers: OnboardingAnswers): boolean {
  return (
    answers.profile.name !== '' ||
    answers.profile.birthDate !== '' ||
    answers.profile.sex !== '' ||
    answers.profile.heightCm !== '' ||
    answers.metrics.choice !== undefined ||
    answers.metrics.measurementSaved ||
    answers.goal.selected !== undefined ||
    answers.training.days.length > 0 ||
    answers.equipment.items.length > 0 ||
    answers.nutrition.preference !== '' ||
    answers.nutrition.restrictions !== ''
  );
}

/**
 * Maps the local draft onto the backend `PATCH /api/v1/profile/onboarding`
 * payload (FOR-121). Values are sent verbatim — no casing/enum transform is
 * needed here (unlike `theme.ts`'s `ThemeMode` mapping), since the backend
 * treats every onboarding field as an unvalidated raw string (verified
 * against `SubmitOnboardingAnswersRequest.toDomain()`).
 */
export function toOnboardingAnswersInput(
  answers: OnboardingAnswers,
  completed: boolean,
): OnboardingAnswersInput {
  return {
    profile: { ...answers.profile },
    metrics: { choice: answers.metrics.choice, measurementSaved: answers.metrics.measurementSaved },
    goal: { selected: answers.goal.selected },
    training: { days: answers.training.days },
    equipment: { items: answers.equipment.items },
    nutrition: { ...answers.nutrition },
    completed,
  };
}

function isBodyMetricsChoice(value: string | undefined): value is BodyMetricsChoice {
  return value === 'MANUAL' || value === 'IMPORT';
}

function isGoalOption(value: string | undefined): value is GoalOption {
  return value === 'COMPOSICION' || value === 'RENDIMIENTO' || value === 'HABITO';
}

/**
 * Maps the backend's onboarding read model back onto the local draft shape —
 * used to recover answers when `localStorage` is empty/cleared but the
 * backend already has prior progress (spec edge case). An unrecognized
 * `choice`/`selected` value (the backend never validates these — they are
 * raw strings) falls back to `undefined` rather than trusting it blindly.
 */
export function fromOnboardingAnswersOutput(output: OnboardingAnswersOutput): OnboardingAnswers {
  return {
    profile: { ...output.profile },
    metrics: {
      choice: isBodyMetricsChoice(output.metrics.choice) ? output.metrics.choice : undefined,
      measurementSaved: output.metrics.measurementSaved,
    },
    goal: { selected: isGoalOption(output.goal.selected) ? output.goal.selected : undefined },
    training: { days: output.training.days },
    equipment: { items: output.equipment.items },
    nutrition: { ...output.nutrition },
  };
}

/**
 * Best-effort background sync of the current progress to the backend
 * (FOR-121): resolves `true` on success and `false` on failure, never
 * throws. Callers must not `await` this before navigating — it is meant to
 * run in the background (Common Pitfall: don't block a step on a slow/failed
 * backend call); `OnboardingPage.tsx` uses the resolved boolean only to
 * decide whether to surface a non-blocking "not saved yet" notice.
 */
export async function syncOnboardingProgress(
  progress: OnboardingProgress,
  client: ApiClient = apiClient,
): Promise<boolean> {
  try {
    await submitOnboardingAnswers(
      toOnboardingAnswersInput(progress.answers, progress.completed),
      client,
    );
    return true;
  } catch {
    return false;
  }
}

/** The backend's onboarding state, as consumed by the first-run gate. */
export interface OnboardingBackendState {
  readonly firstRunCompleted: boolean;
  readonly answers: OnboardingAnswers;
}

/**
 * Fetches the backend's onboarding state (FOR-121's first-run gate source of
 * truth) via `GET /api/v1/profile`. Returns `undefined` on any failure —
 * callers must fall back to the local flag/draft rather than trapping the
 * user in or out of onboarding on a fetch failure.
 */
export async function fetchOnboardingBackendState(
  client: ApiClient = apiClient,
): Promise<OnboardingBackendState | undefined> {
  try {
    const profile = await getProfile(client);
    return {
      firstRunCompleted: profile.firstRunCompleted,
      answers: fromOnboardingAnswersOutput(profile.onboardingAnswers),
    };
  } catch {
    return undefined;
  }
}

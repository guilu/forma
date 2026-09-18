/**
 * Plan-request API calls (ADR-015 slice 3), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). Consumes the ADR-015 slice 2 backend
 * (`PlanRequestController`, `PlanRequestCreateRequest`, `PlanRequestResponse` —
 * verified directly against the backend source): {@code POST
 * /api/v1/plan-requests} and {@code GET /api/v1/plan-requests/current}.
 *
 * <p>This is the wizard's final, additional submission (ADR-015 decision 8) —
 * distinct from `profile.ts`'s `submitOnboardingAnswers`, which keeps
 * persisting the draft's per-step progress exactly as it did before this
 * slice. The Spanish labels the wizard collects (weekday checkboxes,
 * equipment sentences, diet preference) never cross this boundary: they are
 * mapped onto the machine vocabulary below by
 * `pages/onboarding/planRequestMapping.ts` before a {@link
 * CreatePlanRequestInput} is built (ADR-015 decision 6 — the frontend owns
 * this mapping, because no backend endpoint exposes it as a translation
 * service; the backend's own `SpanishWeekdayLabels`/`TrainingEquipment`
 * javadoc document the same table independently, on the other side of the
 * boundary).
 */
import { apiClient, type ApiClient } from './client';
import type { MainGoal } from './profile';

/** Mirrors the backend `PlanDirection` enum (ADR-015 decision 5's amendment). */
export type PlanDirection = 'LOSE_FAT' | 'GAIN_MUSCLE' | 'MAINTAIN';

/** Mirrors the backend `TrainingEquipment` enum — the wizard's six equipment labels, mapped 1:1. */
export type TrainingEquipment =
  | 'BODYWEIGHT'
  | 'DUMBBELLS'
  | 'BARBELL'
  | 'BANDS'
  | 'MACHINES'
  | 'CARDIO_MACHINE';

/** Mirrors the backend `DietPattern` enum — an exclusion rule, orthogonal to {@link CuisineStyle}. */
export type DietPattern = 'OMNIVORE' | 'VEGETARIAN' | 'VEGAN' | 'GLUTEN_FREE' | 'UNSPECIFIED';

/** Mirrors the backend `CuisineStyle` enum — a cuisine, orthogonal to {@link DietPattern}. */
export type CuisineStyle = 'ESPANOLA' | 'MEDITERRANEA' | 'UNSPECIFIED';

/** Mirrors the backend `PlanRequestStatus` enum — the request's lifecycle (ADR-015 decision 2). */
export type PlanRequestStatus = 'PENDING' | 'GENERATING' | 'READY' | 'FAILED';

/** Mirrors the backend `PlanObjective` enum — what THIS plan should do, carrying the arithmetic factor. */
export type PlanObjective = 'WEIGHT_LOSS' | 'MUSCLE_GAIN' | 'MAINTENANCE' | 'HEALTHY_EATING';

/** Mirrors `java.time.DayOfWeek`'s enum names — never the wizard's Spanish weekday label. */
export type DayOfWeekName =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

/**
 * Body accepted by `POST /api/v1/plan-requests` (mirrors
 * `PlanRequestCreateRequest`). Every profile-derived input (sex, height,
 * activity level, birth date, weight) is resolved server-side — this body
 * carries only what the wizard itself answered.
 */
export interface CreatePlanRequestInput {
  readonly direction: PlanDirection;
  readonly trainingDaysPerWeek: number;
  /** `undefined` means nobody said; an empty (non-sent) list is a different, deliberate answer. */
  readonly trainingWeekdays?: readonly DayOfWeekName[];
  readonly equipment?: readonly TrainingEquipment[];
  readonly mealsPerDay: number;
  readonly dietPattern: DietPattern;
  readonly cuisineStyle: CuisineStyle;
}

/**
 * Response body for both endpoints (mirrors `PlanRequestResponse`).
 * Deliberately narrower than the full row — see the backend record's own
 * javadoc for why `requestPayload`/`validationReport`/`failureCode` etc.
 * never appear here.
 */
export interface PlanRequest {
  readonly id: string;
  readonly status: PlanRequestStatus;
  readonly mainGoal: MainGoal;
  readonly planObjective: PlanObjective;
  readonly planKcal: number;
  readonly trainingDaysPerWeek: number;
  readonly trainingWeekdays: readonly DayOfWeekName[];
  readonly equipment: readonly TrainingEquipment[];
  readonly mealsPerDay: number;
  readonly dietPattern: DietPattern;
  readonly cuisineStyle: CuisineStyle;
  readonly requestedAt: string;
}

/**
 * Captures a new plan request — the wizard's final, additional submission
 * (ADR-015 decision 8), fired once on completing the flow.
 *
 * @throws {@link import('./client').ApiRequestError} with status 409 when the
 *   caller already has an open request (ADR-015 decision 4). Callers should
 *   fetch {@link getCurrentPlanRequest} to explain that rather than showing
 *   the raw error.
 * @throws {@link import('./client').ApiRequestError} with status 400 when the
 *   caller's profile/body-measurement data is incomplete — the backend's
 *   `message` already names which piece is missing (`PlanRequestService`).
 */
export function createPlanRequest(
  input: CreatePlanRequestInput,
  client: ApiClient = apiClient,
): Promise<PlanRequest> {
  return client.request<PlanRequest>('/api/v1/plan-requests', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

/**
 * The caller's currently open request, so a 409 from {@link
 * createPlanRequest} can be explained with real data instead of a raw error.
 *
 * @throws {@link import('./client').ApiRequestError} with status 404 when the
 *   caller has none.
 */
export function getCurrentPlanRequest(client: ApiClient = apiClient): Promise<PlanRequest> {
  return client.request<PlanRequest>('/api/v1/plan-requests/current');
}

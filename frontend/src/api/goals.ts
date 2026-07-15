/**
 * Goals & milestones API calls (FOR-122), consuming the FOR-125 backend
 * (`GoalController`, `GoalResponse`, `CreateGoalRequest`, `PatchGoalRequest` —
 * verified directly against `backend/src/main/java/.../delivery/goals/*`, not
 * just `specs/FOR-125/api.md`), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p><b>Enum casing (verified, not assumed):</b> {@link GoalMetric},
 * {@link GoalStatus} and {@link ProgressSource} all mirror the backend's
 * uppercase enum names verbatim (`GoalMetric.name()` / `GoalStatus.name()` /
 * `ProgressSource.name()` in `GoalResponse.from`/`GoalProgressResponse.from`)
 * — same convention as `profile.ts`'s `Sex`/`ActivityLevel`/`MainGoal`.
 *
 * <p><b>Progress is never fabricated (architecture-overview.md, spec FOR-122
 * Non-Functional Requirements: "No goal-progress math client-side"):</b>
 * {@link GoalProgress.current}/{@link GoalProgress.ratio} are explicit `null`
 * when the metric has no linked data yet (`GoalProgressResponse` always
 * returns the object with explicit nulls, never omits it or substitutes 0) —
 * callers must render a neutral "no data" state for `null`, not a fabricated
 * 0%. `ratio` is copied straight from the backend and never recomputed here.
 */
import { apiClient, type ApiClient } from './client';

/** Mirrors the backend `GoalMetric` enum (FOR-125): the initial, closed set. */
export type GoalMetric = 'BODY_FAT_PCT' | 'WEIGHT_KG' | 'LEAN_MASS_KG';

/** Mirrors the backend `GoalStatus` enum (FOR-125): always user-set, never auto-derived. */
export type GoalStatus = 'ACTIVE' | 'ACHIEVED' | 'ARCHIVED';

/** Mirrors the backend `ProgressSource` enum (FOR-125); only one source exists in this slice. */
export type ProgressSource = 'BODY_MEASUREMENT';

/**
 * A goal's derived progress (`GoalProgressResponse`, FOR-125). `current`/
 * `ratio` are `null` (not omitted) when the metric has no linked source or no
 * data yet — never fabricated as 0.
 */
export interface GoalProgress {
  readonly current: number | null;
  readonly target: number;
  readonly ratio: number | null;
  readonly source: ProgressSource;
}

/** A checkpoint on the way to a goal's target (`MilestoneResponse`, FOR-125). */
export interface Milestone {
  readonly id: string;
  readonly title: string;
  readonly target: number;
  readonly completed: boolean;
}

/** A goal with its derived progress and milestones (`GoalResponse`, FOR-125). */
export interface Goal {
  readonly id: string;
  readonly title: string;
  readonly metric: GoalMetric;
  readonly target: number;
  /** ISO-8601 date (`yyyy-MM-dd`), or `null` when the goal has no due date. */
  readonly dueDate: string | null;
  readonly status: GoalStatus;
  readonly progress: GoalProgress;
  readonly milestones: readonly Milestone[];
}

/** Response body for `GET /api/v1/goals` (`GoalsListResponse`, FOR-125): `{"goals": [...]}`. */
export interface GoalsListResponse {
  readonly goals: readonly Goal[];
}

/** A milestone as supplied on `POST /api/v1/goals` (`CreateMilestoneRequest`, FOR-125). */
export interface CreateMilestoneInput {
  readonly title: string;
  readonly target: number;
}

/** Body accepted by `POST /api/v1/goals` (`CreateGoalRequest`, FOR-125). */
export interface CreateGoalInput {
  readonly title: string;
  readonly metric: GoalMetric;
  readonly target: number;
  readonly dueDate?: string;
  readonly milestones?: readonly CreateMilestoneInput[];
}

/**
 * A milestone completion-state change as supplied on `PATCH /api/v1/goals/{id}`
 * (`PatchMilestoneRequest`, FOR-125). Only `completed` can change this way —
 * a milestone is never renamed, retargeted, added or removed via this PATCH.
 */
export interface MilestonePatchInput {
  readonly id: string;
  readonly completed: boolean;
}

/**
 * Body accepted by `PATCH /api/v1/goals/{id}` (`PatchGoalRequest`, FOR-125).
 * Every field is optional/partial — an omitted key leaves the stored value
 * unchanged (mirrors `UpdateProfileFieldsInput`'s merge-not-clobber contract).
 * `metric` is never patchable in this slice.
 */
export interface UpdateGoalInput {
  readonly title?: string;
  readonly target?: number;
  readonly dueDate?: string;
  readonly status?: GoalStatus;
  readonly milestones?: readonly MilestonePatchInput[];
}

/** Lists the owner's goals with derived progress and milestones; empty, never 404. */
export function listGoals(client: ApiClient = apiClient): Promise<readonly Goal[]> {
  return client.request<GoalsListResponse>('/api/v1/goals').then((response) => response.goals);
}

/** Creates a goal, optionally with milestones. */
export function createGoal(input: CreateGoalInput, client: ApiClient = apiClient): Promise<Goal> {
  return client.request<Goal>('/api/v1/goals', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

/** Partially updates a goal's fields and/or milestone completion state. */
export function updateGoal(
  id: string,
  input: UpdateGoalInput,
  client: ApiClient = apiClient,
): Promise<Goal> {
  return client.request<Goal>(`/api/v1/goals/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

/**
 * Training calendar API calls (FOR-26), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as
 * returned; it owns no training rules.
 */
import { apiClient, type ApiClient } from './client';

const TRAINING_WEEK_PATH = '/api/v1/training/week';
const PLAN_RESTART_PATH = '/api/v1/training/plan/restart';

/** Completion status of a training session (FOR-27). */
export type SessionStatus = 'PLANNED' | 'COMPLETED' | 'SKIPPED';

/** A single planned session shown on a calendar day. */
export interface TrainingSession {
  readonly id: string;
  readonly kind: 'RUNNING' | 'STRENGTH';
  readonly title: string;
  readonly detail: string;
  readonly status: SessionStatus;
  readonly notes?: string;
  readonly workoutType?: string;
  readonly bodyView: 'FRONT' | 'BACK';
}

/** One day of the training week; `rest` is true when there are no sessions. */
export interface TrainingDay {
  readonly dayOfWeek: string;
  readonly rest: boolean;
  readonly sessions: TrainingSession[];
}

/**
 * The composed training week (Monday through Sunday).
 *
 * <p>`planState`/`planWeek`/`planTotalWeeks` (design D3 of
 * training-progression-and-logging) say where the account's plan cycle sits:
 * `NOT_STARTED` (never accepted one), `ACTIVE` (mid-cycle, `planWeek` is the
 * 1-based week), or `COMPLETED` (finished; `planWeek` is `null`). Optional
 * here — not because the real API ever omits them, it always sends all
 * three — but because many fixtures across this codebase predate this field
 * and only describe `days`; treat a missing `planState` as "unknown", not as
 * `NOT_STARTED`.
 */
export interface TrainingWeek {
  readonly days: TrainingDay[];
  readonly planState?: 'NOT_STARTED' | 'ACTIVE' | 'COMPLETED';
  readonly planWeek?: number | null;
  readonly planTotalWeeks?: number;
}

/** The updated session status returned by `PATCH …/status` (FOR-27). */
export interface SessionStatusResult {
  readonly id: string;
  readonly status: SessionStatus;
  readonly notes?: string;
}

/** Fetches the current week's training calendar. */
export function getTrainingWeek(client: ApiClient = apiClient): Promise<TrainingWeek> {
  return client.request<TrainingWeek>(TRAINING_WEEK_PATH);
}

/**
 * Starts a new 16-week cycle (design D5 of training-progression-and-logging),
 * for an account whose plan already reached `planState: 'COMPLETED'`. The
 * caller refetches the week afterwards — this call answers with nothing to
 * patch in place, unlike {@link rescheduleSession}.
 */
export function restartPlan(client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(PLAN_RESTART_PATH, { method: 'POST' });
}

/** Marks a session's completion status (FOR-27). */
export function updateSessionStatus(
  sessionId: string,
  status: SessionStatus,
  notes?: string,
  client: ApiClient = apiClient,
): Promise<SessionStatusResult> {
  return client.request<SessionStatusResult>(
    `/api/v1/training/sessions/${encodeURIComponent(sessionId)}/status`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status, notes }),
    },
  );
}

/** A day of the training week, as the backend names them. */
export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/**
 * Moves a session to another day of the current week, or back to its planned
 * day when `day` is null.
 *
 * <p>Answers with the whole redrawn week rather than the moved session: the
 * move changes which day every other session shares it with, so the caller
 * would have to refetch the week anyway.
 */
export function rescheduleSession(
  sessionId: string,
  day: DayOfWeek | null,
  client: ApiClient = apiClient,
): Promise<TrainingWeek> {
  return client.request<TrainingWeek>(
    `/api/v1/training/sessions/${encodeURIComponent(sessionId)}/schedule`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ day }),
    },
  );
}

/**
 * Load level a muscle receives within a strength session (FOR-136), derived
 * server-side from how many of the session's exercises hit it.
 */
export type MuscleLoad = 'HIGH' | 'MEDIUM' | 'LOW';

/**
 * One worked muscle and its derived load (FOR-136). `muscle` is the raw
 * label verbatim from the backend's exercise catalog — lowercase, accented
 * Spanish (e.g. `"hombro"` and `"hombro anterior"` are distinct values). The
 * backend never normalizes this; display grouping/normalization is a UI-layer
 * concern (see `pages/trainingMuscleLabels.ts`).
 */
export interface MuscleWorked {
  readonly muscle: string;
  readonly load: MuscleLoad;
}

/**
 * The worked-muscle map for a training session (FOR-136). `muscles` is empty
 * for a non-strength (running/rest) session — not an error.
 */
export interface MuscleWorkedMap {
  readonly sessionId: string;
  readonly muscles: MuscleWorked[];
}

/** Fetches the worked-muscle map for a session (FOR-136); empty for non-strength sessions. */
export function getMuscleMap(
  sessionId: string,
  client: ApiClient = apiClient,
): Promise<MuscleWorkedMap> {
  return client.request<MuscleWorkedMap>(
    `/api/v1/training/sessions/${encodeURIComponent(sessionId)}/muscle-map`,
  );
}

export interface WorkoutItem {
  readonly exerciseId: string;
  readonly exerciseName: string;
  readonly order: number;
  readonly sets: number;
  readonly repScheme: 'RANGE' | 'AMRAP' | 'TIME_HOLD';
  readonly repsMin?: number;
  readonly repsMax?: number;
  readonly durationSecondsMin?: number;
  readonly durationSecondsMax?: number;
  readonly restSeconds: number;
  readonly rir: number;
}

export interface Workout {
  readonly workoutType: string;
  readonly items: WorkoutItem[];
}

/** Fetches the real exercise prescription for one strength workout template. */
export function getWorkout(type: string, client: ApiClient = apiClient): Promise<Workout> {
  return client.request<Workout>(`/api/v1/training/workouts/${encodeURIComponent(type)}`);
}

/**
 * One set's persisted state, as returned by the per-set log endpoints
 * (training-progression-and-logging slice B, design D6/D7). A not-yet-logged
 * set has `weightKg`/`reps` as `null` and `done` as `false`, distinct from an
 * actual zero.
 */
export interface LoggedSet {
  readonly exerciseId: string;
  readonly setNumber: number;
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly done: boolean;
}

/**
 * The current week's full set grid for one strength session (design D6): one
 * entry per `(exerciseId, setNumber)` the active template prescribes,
 * left-joined with anything already logged. A set the template no longer
 * prescribes (template changed mid-week) is excluded here — the backend
 * neither renders nor deletes it.
 */
export interface SessionSetLog {
  readonly sessionId: string;
  readonly sets: LoggedSet[];
}

/**
 * Fetches the current week's per-set log for a strength session (design D6).
 * 404s for a running session, a session outside the current week, or one
 * whose exercise/set is outside the current template.
 */
export function getSessionSets(
  sessionId: string,
  client: ApiClient = apiClient,
): Promise<SessionSetLog> {
  return client.request<SessionSetLog>(
    `/api/v1/training/sessions/${encodeURIComponent(sessionId)}/sets`,
  );
}

/**
 * What to persist for one set (design D7). `weightKg` and `reps` are each
 * independently optional — logging only the weight is a legitimate partial
 * write, not an error.
 */
export interface LogSetInput {
  readonly weightKg: number | null;
  readonly reps: number | null;
  readonly done: boolean;
}

/**
 * Writes one set — never the whole session (design D7): the write unit
 * matches the user's action (an input losing focus, a toggle), so editing one
 * set never races, nor last-writer-wins over, another set's write. Returns
 * the set as stored.
 */
export function putSessionSet(
  sessionId: string,
  exerciseId: string,
  setNumber: number,
  input: LogSetInput,
  client: ApiClient = apiClient,
): Promise<LoggedSet> {
  return client.request<LoggedSet>(
    `/api/v1/training/sessions/${encodeURIComponent(sessionId)}/sets/${encodeURIComponent(exerciseId)}/${setNumber}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    },
  );
}

/**
 * Progress read models (FOR-139), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). The frontend renders these exactly
 * as returned; it computes no streak/consistency rules of its own (ADR-001 —
 * no progression rules in the UI).
 *
 * <p>Both endpoints are derived by the backend from real per-date **nutrition
 * meal-log** history only (`backend/.../application/StreakService.java` and
 * `WeeklyHistoryService.java`) — there is no per-date training-completion
 * history to back a training streak or a per-week training bar (spec
 * FOR-139: "do NOT fabricate per-date training completion"). Callers must not
 * relabel these as training-specific; render them as the general
 * consistency/streak signal the backend documents.
 */
import { apiClient, type ApiClient } from './client';

const STREAK_PATH = '/api/v1/progress/streak';
const WEEKLY_HISTORY_PATH = '/api/v1/progress/weekly-history';

/**
 * Current + longest consistency streak (FOR-139), as of `asOf` (owner
 * timezone date, ISO `YYYY-MM-DD`). Never absent — an owner with no history
 * yet still gets a zeroed streak (`currentStreakDays: 0, longestStreakDays:
 * 0`), which is a normal empty state, not an error.
 */
export interface Streak {
  readonly currentStreakDays: number;
  readonly longestStreakDays: number;
  readonly asOf: string;
}

/** One week's planned-vs-completed bucket (FOR-139); `weekStart` is the Monday of that week (ISO). */
export interface WeeklyHistoryBucket {
  readonly weekStart: string;
  readonly planned: number;
  readonly completed: number;
}

/**
 * Bounded per-week series (FOR-139), oldest week first, ending with the
 * current week. Weeks with no activity are still present as zero-valued
 * buckets — never omitted — so bars always render for the full window.
 */
export interface WeeklyHistory {
  readonly weeks: WeeklyHistoryBucket[];
}

/** Fetches the current + longest consistency streak (FOR-139, default 90-day lookback). */
export function getStreak(client: ApiClient = apiClient): Promise<Streak> {
  return client.request<Streak>(STREAK_PATH);
}

/** Fetches the bounded per-week history series (FOR-139, default last 8 weeks). */
export function getWeeklyHistory(client: ApiClient = apiClient): Promise<WeeklyHistory> {
  return client.request<WeeklyHistory>(WEEKLY_HISTORY_PATH);
}

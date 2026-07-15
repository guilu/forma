/**
 * Weekly insights API calls (FOR-45), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as returned; the
 * main/secondary recommendations and their priority are computed by the backend, never
 * recomputed here (docs/api/weekly-insights.md).
 */
import { apiClient, type ApiClient } from './client';

const WEEKLY_INSIGHTS_PATH = '/api/v1/insights/weekly';
const INSIGHTS_HISTORY_PATH = '/api/v1/insights/history';

/** The FOR-40 weekly snapshot; absent body values are omitted by the backend. */
export interface WeeklyCheckIn {
  readonly weekStartDate: string;
  readonly latestWeightKg?: number;
  readonly latestBodyFatPercentage?: number;
  readonly latestLeanMassKg?: number;
  readonly plannedRunningSessions: number;
  readonly completedRunningSessions: number;
  readonly plannedStrengthSessions: number;
  readonly completedStrengthSessions: number;
  readonly notes?: string;
}

/** Recommendation category (FOR-41). */
export type RecommendationCategory = 'BODY' | 'TRAINING' | 'NUTRITION' | 'RECOVERY' | 'SHOPPING';

/** Recommendation severity (FOR-41): priority is `ACTION > WARNING > INFO`. */
export type RecommendationSeverity = 'INFO' | 'WARNING' | 'ACTION';

/** An explainable recommendation; `relatedMetric` is omitted when absent. */
export interface Recommendation {
  readonly category: RecommendationCategory;
  readonly severity: RecommendationSeverity;
  readonly message: string;
  readonly reason: string;
  readonly relatedMetric?: string;
  readonly createdAt: string;
}

/**
 * Week-over-week deltas (FOR-110) vs. the immediately prior persisted period.
 * Every field is `undefined`/absent — never a fabricated `0` — when there is
 * no prior period to compare against, or when the underlying value is
 * missing on either side (backend `WeeklyInsightsResponse.Deltas`, which
 * omits null fields from the JSON body via `@JsonInclude(NON_NULL)`).
 *
 * <p>Note: despite `specs/FOR-124/spec.md` describing these as fields "on
 * `WeeklyCheckIn`", the actual FOR-110 backend
 * (`WeeklyInsightsResponse.java`) places them on a sibling top-level
 * `deltas` object instead — the repository is the source of truth here, so
 * this type mirrors the real response shape.
 */
export interface WeeklyInsightsDeltas {
  readonly weightDeltaKg?: number;
  readonly bodyFatPercentageDelta?: number;
  readonly leanMassDeltaKg?: number;
  readonly trainingCompletionDelta?: number;
}

/**
 * The current week's insights: check-in snapshot, the highest-priority `main`
 * recommendation (always present — empty underlying data still yields an
 * insufficient-data `INFO` recommendation, never a 4xx/5xx), any `secondary`
 * recommendations, when they were generated, and its week-over-week
 * `deltas` (FOR-110) vs. the immediately prior persisted period. Each entry
 * of {@link getInsightsHistory}'s response shares this exact shape, keyed by
 * `checkIn.weekStartDate` as its period (no separate `period` field exists
 * on the backend response).
 */
export interface WeeklyInsights {
  readonly checkIn: WeeklyCheckIn;
  readonly main: Recommendation;
  readonly secondary: Recommendation[];
  readonly generatedAt: string;
  readonly deltas?: WeeklyInsightsDeltas;
}

/** Fetches the current week's insights (check-in + prioritized recommendations). */
export function getWeeklyInsights(client: ApiClient = apiClient): Promise<WeeklyInsights> {
  return client.request<WeeklyInsights>(WEEKLY_INSIGHTS_PATH);
}

/**
 * Fetches every persisted period's insights (FOR-110), most recent first.
 * Empty array when nothing has been generated yet — not an error. The
 * backend computes ordering and deltas; this never re-sorts or re-derives
 * either client-side (ADR-006).
 */
export function getInsightsHistory(client: ApiClient = apiClient): Promise<WeeklyInsights[]> {
  return client.request<WeeklyInsights[]>(INSIGHTS_HISTORY_PATH);
}

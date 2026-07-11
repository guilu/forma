/**
 * Weekly insights API calls (FOR-45), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as returned; the
 * main/secondary recommendations and their priority are computed by the backend, never
 * recomputed here (docs/api/weekly-insights.md).
 */
import { apiClient, type ApiClient } from './client';

const WEEKLY_INSIGHTS_PATH = '/api/v1/insights/weekly';

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
 * The current week's insights: check-in snapshot, the highest-priority `main`
 * recommendation (always present — empty underlying data still yields an
 * insufficient-data `INFO` recommendation, never a 4xx/5xx), any `secondary`
 * recommendations, and when they were generated.
 */
export interface WeeklyInsights {
  readonly checkIn: WeeklyCheckIn;
  readonly main: Recommendation;
  readonly secondary: Recommendation[];
  readonly generatedAt: string;
}

/** Fetches the current week's insights (check-in + prioritized recommendations). */
export function getWeeklyInsights(client: ApiClient = apiClient): Promise<WeeklyInsights> {
  return client.request<WeeklyInsights>(WEEKLY_INSIGHTS_PATH);
}

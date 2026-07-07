/**
 * Training calendar API calls (FOR-26), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as
 * returned; it owns no training rules.
 */
import { apiClient, type ApiClient } from './client';

const TRAINING_WEEK_PATH = '/api/v1/training/week';

/** A single planned session shown on a calendar day. */
export interface TrainingSession {
  readonly kind: 'RUNNING' | 'STRENGTH';
  readonly title: string;
  readonly detail: string;
  readonly status: string;
}

/** One day of the training week; `rest` is true when there are no sessions. */
export interface TrainingDay {
  readonly dayOfWeek: string;
  readonly rest: boolean;
  readonly sessions: TrainingSession[];
}

/** The composed training week (Monday through Sunday). */
export interface TrainingWeek {
  readonly days: TrainingDay[];
}

/** Fetches the current week's training calendar. */
export function getTrainingWeek(client: ApiClient = apiClient): Promise<TrainingWeek> {
  return client.request<TrainingWeek>(TRAINING_WEEK_PATH);
}

/**
 * Training calendar API calls (FOR-26), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as
 * returned; it owns no training rules.
 */
import { apiClient, type ApiClient } from './client';

const TRAINING_WEEK_PATH = '/api/v1/training/week';

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

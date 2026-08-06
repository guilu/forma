/**
 * Plan acceptance (V58): whether a plan is written and waiting to be started, and starting it.
 *
 * <p>Built on the shared {@link apiClient} boundary (ADR-006 — no ad-hoc `fetch`).
 */
import { apiClient, type ApiClient } from './client';

/**
 * What the app is offered on first login.
 *
 * <p>`planName` is absent when there is nothing to offer — either the account already accepted, or
 * it has no plan at all. The second case is not an error: it is an account whose plan generation
 * produced nothing, and the screens say so rather than offering a plan that is not there.
 */
export interface PlanAcceptance {
  readonly pending: boolean;
  readonly planName?: string;
}

const PATH = '/api/v1/plan-acceptance';

/** Whether a plan is waiting to be started. Never 404s. */
export function getPlanAcceptance(client: ApiClient = apiClient): Promise<PlanAcceptance> {
  return client.request<PlanAcceptance>(PATH);
}

/** Starts the plan: the server activates it and records the acceptance in one go. */
export function acceptPlan(client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(PATH, { method: 'POST' });
}

/**
 * Body measurements API calls (FOR-18), built on the shared {@link apiClient}
 * boundary (ADR-006 — no ad-hoc `fetch`). Types mirror the FOR-17 contract
 * (specs/FOR-17/api.md); the frontend never recomputes derived values, it only
 * reads them from the response.
 */
import { apiClient, type ApiClient } from './client';

const MEASUREMENTS_PATH = '/api/v1/body/measurements';

/** Request body for `POST /api/v1/body/measurements` (`source` is server-set to MANUAL). */
export interface CreateBodyMeasurementRequest {
  readonly measuredAt: string;
  readonly weightKg: number;
  readonly bodyFatPercentage: number;
  readonly bmi: number;
  readonly notes?: string;
}

/** A body measurement as returned by the API, including backend-derived masses. */
export interface BodyMeasurement {
  readonly measuredAt: string;
  readonly source: string;
  readonly weightKg: number;
  readonly bodyFatPercentage: number;
  readonly bmi: number;
  readonly fatMassKg?: number;
  readonly leanMassKg?: number;
  readonly notes?: string;
}

/** Creates a manually entered measurement and returns the created resource. */
export function createBodyMeasurement(
  request: CreateBodyMeasurementRequest,
  client: ApiClient = apiClient,
): Promise<BodyMeasurement> {
  return client.request<BodyMeasurement>(MEASUREMENTS_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
}

/** Lists measurements, most recent first (FOR-16/FOR-17 default order). */
export function listBodyMeasurements(client: ApiClient = apiClient): Promise<BodyMeasurement[]> {
  return client.request<BodyMeasurement[]>(MEASUREMENTS_PATH);
}

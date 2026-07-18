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
 *
 * <p>FOR-144 adds the progress-photos calls, consuming the FOR-140 backend
 * (`ProgressPhotoController`/`ProgressPhotoResponse`/`ProgressPhotoListResponse`
 * — verified directly against `backend/.../delivery/progress/*`, not just the
 * spec doc). Photos are privacy-sensitive, owner-scoped, access-controlled
 * data (spec FOR-140): no response here ever carries a URL field, and the
 * binary is only ever reachable through {@link fetchProgressPhotoBlob}, which
 * goes through the authenticated {@link ApiClient#requestBlob} — never a
 * public/static `<img src>`. Callers must turn the resolved `Blob` into an
 * object URL (`URL.createObjectURL`) and revoke it when no longer displayed,
 * to avoid leaking memory or exposing the bytes past the component's life.
 */
import { apiClient, type ApiClient } from './client';

const STREAK_PATH = '/api/v1/progress/streak';
const WEEKLY_HISTORY_PATH = '/api/v1/progress/weekly-history';
const PHOTOS_PATH = '/api/v1/progress/photos';

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

/**
 * A progress photo's metadata (`ProgressPhotoResponse`, FOR-140). Deliberately
 * carries no URL field — the backend never surfaces one; the binary is only
 * reachable through {@link fetchProgressPhotoBlob}.
 */
export interface ProgressPhoto {
  readonly id: string;
  readonly contentType: string;
  readonly sizeBytes: number;
  readonly createdAt: string;
}

interface ProgressPhotoListResponse {
  readonly photos: ProgressPhoto[];
}

/**
 * Lists the owner's progress photos, metadata only (`GET /progress/photos`).
 * An owner with no photos yet resolves to an empty array — a normal empty
 * state, never an error (spec FOR-140: "Empty list … → 200 empty, never 404").
 */
export function listProgressPhotos(client: ApiClient = apiClient): Promise<ProgressPhoto[]> {
  return client.request<ProgressPhotoListResponse>(PHOTOS_PATH).then((response) => response.photos);
}

/**
 * Uploads a progress photo (`POST /progress/photos`, `multipart/form-data`,
 * part name `file` — matches `ProgressPhotoController#upload`'s
 * `@RequestParam("file")`). No `Content-Type` header is set explicitly: the
 * browser must compute the multipart boundary itself for a `FormData` body,
 * so hardcoding the header here would corrupt the request.
 *
 * <p>Client-side, only the file picker's `accept` attribute narrows the
 * choice (`image/jpeg`/`image/png`, wired by the calling component) — the
 * actual content-type allow-list and 5 MB size limit are enforced by
 * `ProgressPhotoService` (spec FOR-140), which rejects with `400
 * VALIDATION_ERROR`. Duplicating that check here would duplicate a backend
 * business rule in the UI (ADR-006); callers instead catch the propagated
 * {@link ApiRequestError} and show its `message` directly.
 */
export function uploadProgressPhoto(
  file: File,
  client: ApiClient = apiClient,
): Promise<ProgressPhoto> {
  const formData = new FormData();
  formData.append('file', file);
  return client.request<ProgressPhoto>(PHOTOS_PATH, { method: 'POST', body: formData });
}

/**
 * Deletes a progress photo by id (`DELETE /progress/photos/{id}`, → 204).
 * Resolves to `undefined` — {@link ApiClient#request} treats a 204 response
 * as having no body (FOR-144) rather than trying to parse one as JSON.
 */
export function deleteProgressPhoto(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${PHOTOS_PATH}/${id}`, { method: 'DELETE' });
}

/**
 * Fetches one photo's raw bytes (`GET /progress/photos/{id}`) as a `Blob`,
 * through the authenticated {@link ApiClient#requestBlob} boundary — never a
 * public/static URL (spec FOR-140: owner-scoped, access-controlled binary
 * retrieval; 403 on another owner's photo). Callers render it via
 * `URL.createObjectURL(blob)` and must revoke that object URL when it is no
 * longer displayed (e.g. on unmount) to avoid leaking memory.
 */
export function fetchProgressPhotoBlob(id: string, client: ApiClient = apiClient): Promise<Blob> {
  return client.requestBlob(`${PHOTOS_PATH}/${id}`);
}

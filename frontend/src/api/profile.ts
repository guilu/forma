/**
 * User profile & preferences API calls (FOR-119, extended by FOR-120), built
 * on the shared {@link apiClient} boundary (ADR-006 — no ad-hoc `fetch`).
 * Consumes the FOR-107 backend (`UserProfileController`,
 * `UserProfileResponse`, `UpdateProfileFieldsRequest`,
 * `UpdateThemeModeRequest` — verified directly against the backend source,
 * not just its spec): {@code GET /api/v1/profile}, {@code PATCH
 * /api/v1/profile} and {@code PATCH /api/v1/profile/theme}. The `/units`,
 * `/objectives` and `/onboarding` scoped update endpoints belong to their own
 * owning stories (units stay read-only for the MVP per FOR-107's
 * single-supported-value enums; onboarding is FOR-121) and are intentionally
 * not called here.
 *
 * <p><b>Enum casing (verified, not assumed):</b> unlike {@link Sex}, {@link
 * ActivityLevel} and {@link MainGoal} (which mirror the backend's uppercase
 * strings verbatim — the onboarding flow's `GoalOption`,
 * `onboardingStorage.ts` FOR-59, already uses the same vocabulary), {@code
 * ThemeMode} is lowercase in the frontend (`frontend/src/theme/theme.ts`) but
 * uppercase `LIGHT|DARK|SYSTEM` on the API — a documented FOR-107 gotcha.
 * {@link theme.ts}'s `toApiThemeMode`/`fromApiThemeMode` do the explicit case
 * mapping at this boundary; this module never invents its own conversion.
 */
import { apiClient, type ApiClient } from './client';
import type { BackendThemeMode } from '../theme/theme';

/** Mirrors the backend `Sex` enum (`MALE`, `FEMALE`, `OTHER`). */
export type Sex = 'MALE' | 'FEMALE' | 'OTHER';

/** Mirrors the backend `ActivityLevel` enum. */
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE';

/** Mirrors the backend `MainGoal` enum; same vocabulary as onboarding's `GoalOption`. */
export type MainGoal = 'COMPOSICION' | 'RENDIMIENTO' | 'HABITO';

/**
 * Weight/height/distance/energy unit preferences (FOR-107). Each dimension's
 * backend enum currently defines exactly one supported value (metric-only
 * MVP) — the type reflects that today, not a hardcoded assumption: it widens
 * automatically if a future story adds more values to the backend enums.
 */
export interface UnitPreferences {
  readonly weightUnit: 'KG';
  readonly heightUnit: 'CM';
  readonly distanceUnit: 'KM';
  readonly energyUnit: 'KCAL';
}

/**
 * The profile & preferences read model (`GET /api/v1/profile`). The
 * "Profile fields" + `unitPreferences` sections (FOR-119) and `themeMode`
 * (FOR-120) are typed here — the response also carries `defaultObjectives`
 * and `onboardingAnswers`, owned by other stories (ObjectivesSection,
 * FOR-121) and intentionally left untyped/unused by this client to avoid
 * speculative coupling (AGENTS.md: no speculative abstractions). Profile
 * fields are all optional: a fresh, never-saved profile returns them omitted
 * (backend `@JsonInclude(NON_NULL)`), never fabricated placeholders.
 * `themeMode` (like `unitPreferences`) is never omitted — the domain
 * aggregate always defaults it (FOR-107).
 */
export interface UserProfile {
  readonly name?: string;
  readonly email?: string;
  /** ISO-8601 date (`yyyy-MM-dd`), matching `<input type="date">`'s value format. */
  readonly birthDate?: string;
  readonly sex?: Sex;
  readonly heightCm?: number;
  readonly activityLevel?: ActivityLevel;
  readonly mainGoal?: MainGoal;
  readonly unitPreferences: UnitPreferences;
  /** Theme preference (FOR-120), backend vocabulary — map via `theme.ts`'s helpers. */
  readonly themeMode: BackendThemeMode;
}

/** Fields accepted by `PATCH /api/v1/profile` — every field optional/partial (FOR-107). */
export interface UpdateProfileFieldsInput {
  readonly name?: string;
  readonly email?: string;
  readonly birthDate?: string;
  readonly sex?: Sex;
  readonly heightCm?: number;
  readonly activityLevel?: ActivityLevel;
  readonly mainGoal?: MainGoal;
}

/** Fetches the profile & preferences aggregate, with FOR-107's first-run defaults. */
export function getProfile(client: ApiClient = apiClient): Promise<UserProfile> {
  return client.request<UserProfile>('/api/v1/profile');
}

/**
 * Partially updates the "Profile fields" section; an omitted key leaves the
 * stored value unchanged (FOR-107 merge-not-clobber contract) — callers
 * build {@link UpdateProfileFieldsInput} with `undefined` for anything they
 * don't want to change, which `JSON.stringify` drops from the request body.
 */
export function updateProfileFields(
  input: UpdateProfileFieldsInput,
  client: ApiClient = apiClient,
): Promise<UserProfile> {
  return client.request<UserProfile>('/api/v1/profile', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

/** Body accepted by `PATCH /api/v1/profile/theme` — single-valued, required (FOR-107). */
export interface UpdateThemeModeInput {
  readonly themeMode: BackendThemeMode;
}

/**
 * Persists the theme preference (FOR-120). Callers pass the backend's
 * uppercase vocabulary — map from the frontend's lowercase `ThemeMode` via
 * `theme.ts`'s `toApiThemeMode` first; this client never does the case
 * conversion itself (single responsibility: HTTP call only).
 */
export function updateThemeMode(
  input: UpdateThemeModeInput,
  client: ApiClient = apiClient,
): Promise<UserProfile> {
  return client.request<UserProfile>('/api/v1/profile/theme', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

/**
 * User profile & preferences API calls (FOR-119), built on the shared
 * {@link apiClient} boundary (ADR-006 — no ad-hoc `fetch`). Consumes the
 * FOR-107 backend (`UserProfileController`, `UserProfileResponse`,
 * `UpdateProfileFieldsRequest` — verified directly against the backend
 * source, not just its spec): {@code GET /api/v1/profile} and
 * {@code PATCH /api/v1/profile}. This story only wires the "Profile fields"
 * section; the `/units`, `/objectives`, `/theme` and `/onboarding` scoped
 * update endpoints belong to their own owning stories (units stay
 * read-only for the MVP per FOR-107's single-supported-value enums; theme is
 * FOR-120; onboarding is FOR-121) and are intentionally not called here.
 *
 * <p><b>Enum casing (verified, not assumed):</b> unlike `ThemeMode` (lowercase
 * in the frontend, uppercase `LIGHT|DARK|SYSTEM` on the API — a documented
 * FOR-107 gotcha), {@link Sex}, {@link ActivityLevel} and {@link MainGoal}
 * have no separate lowercase frontend vocabulary to map from/to: the
 * onboarding flow's `GoalOption` (`onboardingStorage.ts`, FOR-59) already
 * uses the exact same uppercase strings (`COMPOSICION | RENDIMIENTO |
 * HABITO`) the backend returns, so these types mirror the backend's
 * `UpdateProfileFieldsRequest` `@Pattern` values verbatim — no case
 * conversion layer needed.
 */
import { apiClient, type ApiClient } from './client';

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
 * The profile & preferences read model (`GET /api/v1/profile`). Only the
 * "Profile fields" + `unitPreferences` sections this story consumes are
 * typed here — the response also carries `defaultObjectives`, `themeMode`
 * and `onboardingAnswers`, owned by other stories (ObjectivesSection,
 * FOR-120, FOR-121) and intentionally left untyped/unused by this client to
 * avoid speculative coupling (AGENTS.md: no speculative abstractions).
 * Profile fields are all optional: a fresh, never-saved profile returns them
 * omitted (backend `@JsonInclude(NON_NULL)`), never fabricated placeholders.
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

/**
 * User profile & preferences API calls (FOR-119, extended by FOR-120 and
 * FOR-121), built on the shared {@link apiClient} boundary (ADR-006 — no
 * ad-hoc `fetch`). Consumes the FOR-107 backend (`UserProfileController`,
 * `UserProfileResponse`, `UpdateProfileFieldsRequest`,
 * `UpdateThemeModeRequest`, `SubmitOnboardingAnswersRequest` — verified
 * directly against the backend source, not just its spec): {@code GET
 * /api/v1/profile}, {@code PATCH /api/v1/profile}, {@code PATCH
 * /api/v1/profile/theme} and {@code PATCH /api/v1/profile/onboarding}. The
 * `/units` and `/objectives` scoped update endpoints belong to their own
 * owning stories (units stay read-only for the MVP per FOR-107's
 * single-supported-value enums) and are intentionally not called here.
 *
 * <p><b>Enum casing (verified, not assumed):</b> unlike {@link Sex}, {@link
 * ActivityLevel} and {@link MainGoal} (which mirror the backend's uppercase
 * strings verbatim — the onboarding flow's `GoalOption`,
 * `onboardingStorage.ts` FOR-59, already uses the same vocabulary), {@code
 * ThemeMode} is lowercase in the frontend (`frontend/src/theme/theme.ts`) but
 * uppercase `LIGHT|DARK|SYSTEM` on the API — a documented FOR-107 gotcha.
 * {@link theme.ts}'s `toApiThemeMode`/`fromApiThemeMode` do the explicit case
 * mapping at this boundary; this module never invents its own conversion.
 * {@code /onboarding}'s fields (verified against
 * `SubmitOnboardingAnswersRequest.toDomain()` and the domain
 * `OnboardingAnswers`) have **no such gotcha**: `profile.sex`,
 * `metrics.choice`, `goal.selected`, `training.days`, `equipment.items` and
 * `nutrition.preference` are all passed straight through as unvalidated raw
 * strings (never coerced through a Java enum), so `onboardingStorage.ts` can
 * send its draft answers verbatim.
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
 * Per-step onboarding draft answers as returned by the profile read model
 * (FOR-121, mirrors `UserProfileResponse.OnboardingAnswersResponse`). Every
 * group is always present (the domain `OnboardingAnswers` record defaults a
 * missing group to its blank form rather than `null`), but `metrics.choice`
 * and `goal.selected` are individually omitted when unset (backend
 * `@JsonInclude(NON_NULL)` on those two nested records only).
 */
export interface OnboardingAnswersOutput {
  readonly profile: {
    readonly name: string;
    readonly birthDate: string;
    readonly sex: string;
    readonly heightCm: string;
  };
  readonly metrics: { readonly choice?: string; readonly measurementSaved: boolean };
  readonly goal: { readonly selected?: string };
  readonly training: { readonly days: readonly string[] };
  readonly equipment: { readonly items: readonly string[] };
  readonly nutrition: { readonly preference: string; readonly restrictions: string };
}

/**
 * The profile & preferences read model (`GET /api/v1/profile`). The
 * "Profile fields" + `unitPreferences` sections (FOR-119), `themeMode`
 * (FOR-120) and `onboardingAnswers`/`firstRunCompleted` (FOR-121) are typed
 * here — `defaultObjectives`, owned by `ObjectivesSection`, is intentionally
 * left untyped/unused by this client to avoid speculative coupling
 * (AGENTS.md: no speculative abstractions). Profile fields are all optional:
 * a fresh, never-saved profile returns them omitted (backend
 * `@JsonInclude(NON_NULL)`), never fabricated placeholders. `themeMode`,
 * `unitPreferences`, `onboardingAnswers` and `firstRunCompleted` are never
 * omitted — the domain aggregate always defaults them (FOR-107).
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
  /** Onboarding draft answers (FOR-121) — see {@link OnboardingAnswersOutput}. */
  readonly onboardingAnswers: OnboardingAnswersOutput;
  /** First-run completion flag (FOR-121) — the onboarding gate's source of truth. */
  readonly firstRunCompleted: boolean;
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

/**
 * La petición en vuelo, para que tres pantallas que arrancan a la vez no la pidan tres
 * veces.
 *
 * <p>NO es una caché: se suelta en cuanto la respuesta llega, así que la siguiente llamada
 * vuelve a preguntar al servidor. Cachear el perfil obligaría a invalidarlo cada vez que se
 * guarda —ajustes, unidades, tema, onboarding— y un perfil viejo enseñado después de
 * guardarlo es peor defecto que el que se venía a arreglar. Lo único que se comparte es la
 * ventana en la que la pregunta ya está hecha y aún no ha vuelto.
 */
/**
 * Cuánto vale el perfil recién traído antes de volver a preguntar.
 *
 * <p>El hueco que hay que cubrir es el de una carga de página: `ThemeProvider` monta arriba
 * del todo y pide el perfil de inmediato, mientras que la página va en un trozo aparte
 * (`app/routes.tsx` la carga con `lazy`) y no monta hasta que ese trozo llega. Medido en
 * producción, esas dos peticiones salían con segundos de diferencia — por eso fundir solo
 * las simultáneas no las juntaba: cuando salía la segunda, la primera ya había respondido.
 *
 * <p>Y por eso tiene fecha de caducidad en vez de durar lo que dure la pestaña. Todo lo que
 * cambia el perfil pasa por este módulo y deja aquí el resultado, así que dentro de una
 * pestaña nunca se lee algo viejo; el caso que no cubre es otra pestaña —o el móvil— tocando
 * el perfil a la vez. Sin caducidad, esa pestaña enseñaría lo de antes hasta que alguien
 * recargara. Treinta segundos cubren de sobra la carga más lenta y dejan esa ventana corta.
 */
const FRESH_FOR_MS = 30_000;

let inFlight: Promise<UserProfile> | undefined;
let cached: { readonly profile: UserProfile; readonly at: number } | undefined;

/**
 * Guarda lo último que dijo el servidor.
 *
 * <p>Lo llaman también los tres que ESCRIBEN, que devuelven el perfil ya actualizado: así un
 * guardado no invalida la copia, la sustituye, y quien pregunte justo después recibe lo que
 * se acaba de guardar sin un viaje de más.
 */
function remember(profile: UserProfile): UserProfile {
  cached = { profile, at: Date.now() };
  return profile;
}

/**
 * Solo el cliente por defecto comparte copia. `client` existe para inyectar uno falso en las
 * pruebas, y una copia compartida entre clientes distintos haría que un test leyera la
 * respuesta preparada para otro.
 */
function shared(client: ApiClient): boolean {
  return client === apiClient;
}

/**
 * Fetches the profile & preferences aggregate, with FOR-107's first-run defaults.
 *
 * <p>Tres cosas distintas quieren un trozo distinto del mismo perfil al abrir la aplicación:
 * `ThemeContext` el modo de tema, `useAnatomySex` el sexo para las siluetas y `DashboardPage`
 * el nombre del saludo. Ninguna puede recibirlo por props —dos son un proveedor y un hook, y
 * el proveedor se aplica antes del primer pintado— así que la coincidencia se resuelve aquí,
 * donde las tres pasan igualmente.
 *
 * <p>Dos mecanismos, para dos formas de coincidir: {@link cached} para las que se separan en
 * el tiempo (ver {@link FRESH_FOR_MS}) y {@link inFlight} para las verdaderamente a la vez,
 * que si no saldrían las dos antes de que ninguna haya podido guardar nada.
 */
export function getProfile(client: ApiClient = apiClient): Promise<UserProfile> {
  if (!shared(client)) {
    return client.request<UserProfile>('/api/v1/profile');
  }
  if (cached && Date.now() - cached.at < FRESH_FOR_MS) {
    return Promise.resolve(cached.profile);
  }
  if (inFlight) {
    return inFlight;
  }
  const request = client.request<UserProfile>('/api/v1/profile').then(remember);
  inFlight = request;
  request
    .finally(() => {
      if (inFlight === request) {
        inFlight = undefined;
      }
    })
    .catch(() => {
      // El fallo lo recibe quien llamó, por `request`. Este `catch` es solo para la promesa
      // que encadena `finally`, que si no queda sin manejar y el runtime avisa de un rechazo
      // que en realidad sí tiene dueño.
    });
  return request;
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
  return client
    .request<UserProfile>('/api/v1/profile', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    .then((profile) => (shared(client) ? remember(profile) : profile));
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
  return client
    .request<UserProfile>('/api/v1/profile/theme', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    .then((profile) => (shared(client) ? remember(profile) : profile));
}

/**
 * Body accepted by `PATCH /api/v1/profile/onboarding` (FOR-121, mirrors
 * `SubmitOnboardingAnswersRequest`). Unlike the other profile PATCH
 * endpoints, this is a full replace of the stored onboarding draft, not a
 * per-field merge — an omitted group resets to blank on the backend, so
 * callers (`onboardingStorage.ts`) send the whole current draft every time.
 */
export interface OnboardingAnswersInput {
  readonly profile?: {
    readonly name?: string;
    readonly birthDate?: string;
    readonly sex?: string;
    readonly heightCm?: string;
  };
  readonly metrics?: {
    readonly choice?: string;
    readonly measurementSaved?: boolean;
  };
  readonly goal?: {
    readonly selected?: string;
  };
  readonly training?: {
    readonly days?: readonly string[];
  };
  readonly equipment?: {
    readonly items?: readonly string[];
  };
  readonly nutrition?: {
    readonly preference?: string;
    readonly restrictions?: string;
  };
  readonly completed: boolean;
}

/**
 * Submits the onboarding draft + completion flag (FOR-121) — the exact swap
 * `onboardingStorage.ts`'s original design comment anticipated ("swapping it
 * for a real `PATCH /api/v1/onboarding` call later touches one file").
 * Re-submitting after `completed: true` is allowed (FOR-107 Edge Cases:
 * treated as a profile edit, never locked) — this client enforces no
 * state-machine transition, same as the backend.
 */
export function submitOnboardingAnswers(
  input: OnboardingAnswersInput,
  client: ApiClient = apiClient,
): Promise<UserProfile> {
  return client
    .request<UserProfile>('/api/v1/profile/onboarding', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    .then((profile) => (shared(client) ? remember(profile) : profile));
}

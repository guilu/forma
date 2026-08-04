/**
 * Nutrition API calls (FOR-34), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model as returned;
 * it owns no nutrition rules and never recomputes macros.
 */
import { apiClient, type ApiClient } from './client';

/** A food entry within a meal. */
export interface NutritionItem {
  readonly food: string;
  readonly quantityG: number;
}

/** A meal in the day's flow; `optional` marks a skippable item (e.g. post-run recovery). */
export interface NutritionMeal {
  readonly mealType: string;
  readonly name: string;
  readonly preferredTime: string;
  readonly optional: boolean;
  /** What its items add up to, computed by the server against today's catalog. */
  readonly totals: NutritionTotals;
  readonly items: NutritionItem[];
}

/**
 * Computed macro totals.
 *
 * <p>Declared late, and that is worth saying: the API has returned these for a day
 * and for each of its meals since FOR-105, and this file never described them — so
 * the nutrition page drew invented numbers beside real food for want of a type. The
 * data was arriving the whole time.
 */
export interface NutritionTotals {
  readonly calories: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

/** Daily macro targets. */
export interface NutritionTargets {
  readonly calories: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

/** A day of the plan being followed: what it aims for, what it comes to, and its meals. */
export interface NutritionDay {
  readonly type: string;
  /** What the day was ASKED to hit. A decision, and nobody can compute it. */
  readonly targets: NutritionTargets;
  /** What its meals actually add up to. Not the same thing, and that is the point. */
  readonly totals: NutritionTotals;
  readonly targetComparison: {
    readonly caloriesReached: boolean;
    readonly proteinReached: boolean;
    readonly carbsReached: boolean;
    readonly fatReached: boolean;
  };
  readonly meals: NutritionMeal[];
}

/** Fetches the seeded nutrition day for a type (e.g. `running`). */
export function getNutritionDay(
  type: string,
  client: ApiClient = apiClient,
): Promise<NutritionDay> {
  return client.request<NutritionDay>(`/api/v1/nutrition/days/${encodeURIComponent(type)}`);
}

/**
 * What a day's consumption looks like (FOR-127/FOR-134, plan adherence added by
 * V55).
 *
 * <p>These endpoints have existed since FOR-127 with no screen calling them.
 * `MealLogPanel` is that screen.
 */
export interface ConsumedMacros {
  readonly kcal: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

/**
 * Fibre, sugars, sodium and saturated fat. Any field may be `null`: a nutrient a
 * food genuinely lacks is unknown, never fabricated as 0, and a day's total is
 * null if any of its entries lacks it.
 */
export interface KeyNutrients {
  readonly fiberG: number | null;
  readonly sugarsG: number | null;
  readonly sodiumMg: number | null;
  readonly saturatedFatG: number | null;
}

export interface LoggedEntry {
  readonly id: string;
  readonly mealType: string;
  readonly name: string;
  readonly kcal: number;
}

/**
 * One of the plan's meals for that day and what has become of it.
 *
 * <p>Derived by the server on every read from the entries pointing at it, never
 * stored — `PENDING` becomes `SKIPPED` on its own once the day has passed.
 */
export interface PlannedMealState {
  readonly id: string;
  readonly mealType: string;
  readonly name: string;
  readonly optional: boolean;
  readonly state: 'EATEN' | 'PENDING' | 'SKIPPED';
}

export interface DayConsumption {
  readonly date: string;
  readonly dayType: string | null;
  readonly consumed: ConsumedMacros;
  readonly keyNutrients: KeyNutrients;
  /** What the plan asks of this kind of day, or `null` when nobody has set one. */
  readonly target: ConsumedMacros | null;
  readonly comparison: {
    readonly caloriesReached: boolean;
    readonly proteinReached: boolean;
    readonly carbsReached: boolean;
    readonly fatReached: boolean;
  } | null;
  readonly entries: readonly LoggedEntry[];
  /** Empty when there is no active plan. */
  readonly plannedMeals: readonly PlannedMealState[];
}

/**
 * What to log. A catalog food OR free macros, never both.
 *
 * <p>For a catalog food, the amount is said in exactly one of three ways, the same
 * three a plan line uses: `grams`, or `portions` of a named `servingId`, or
 * `portions` of the food's default one.
 */
export interface LogMealBody {
  readonly date: string;
  readonly mealType: string;
  readonly foodItemId?: string;
  readonly grams?: number;
  readonly portions?: number;
  readonly servingId?: string;
  readonly name?: string;
  readonly kcal?: number;
  readonly proteinG?: number;
  readonly carbsG?: number;
  readonly fatG?: number;
  /** Which planned meal this answers (V55). Absent for an unplanned entry. */
  readonly plannedMealId?: string;
}

export interface LoggedMeal {
  readonly id: string;
  readonly date: string;
  readonly mealType: string;
  readonly name: string;
  readonly kcal: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

/** What was consumed on a day, versus what the plan asked for. Never 404s. */
export function getDayConsumption(
  date: string,
  client: ApiClient = apiClient,
): Promise<DayConsumption> {
  return client.request<DayConsumption>(
    `/api/v1/nutrition/consumption?date=${encodeURIComponent(date)}`,
  );
}

/** Records something eaten. The macros come back computed; nothing here sends them. */
export function logMeal(body: LogMealBody, client: ApiClient = apiClient): Promise<LoggedMeal> {
  return client.request<LoggedMeal>('/api/v1/nutrition/log', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

/**
 * Hydration (FOR-130).
 *
 * <p>These have existed since FOR-130 and nothing had ever called them: the water
 * tile on the dashboard rendered four invented numbers while its own comment said
 * no hydration endpoint existed. It did.
 */
export interface HydrationProgress {
  readonly date: string;
  /** Everything logged that day, in millilitres. */
  readonly totalMl: number;
  /**
   * The daily goal in millilitres, from the profile — or `null` if it cannot be
   * resolved at all, which the API documents and does not currently produce.
   */
  readonly goalMl: number | null;
  /** `totalMl / goalMl`, uncapped: 1.2 means twenty per cent past the goal. */
  readonly progress: number | null;
}

export interface LoggedWaterIntake {
  readonly id: string;
  readonly date: string;
  readonly volumeMl: number;
}

/** What was drunk on a day, against the goal. Never 404s — an empty day is zero. */
export function getHydration(
  date: string,
  client: ApiClient = apiClient,
): Promise<HydrationProgress> {
  return client.request<HydrationProgress>(
    `/api/v1/nutrition/hydration?date=${encodeURIComponent(date)}`,
  );
}

/** Records a volume drunk. Millilitres, because that is what the API counts in. */
export function logWaterIntake(
  date: string,
  volumeMl: number,
  client: ApiClient = apiClient,
): Promise<LoggedWaterIntake> {
  return client.request<LoggedWaterIntake>('/api/v1/nutrition/hydration', {
    method: 'POST',
    body: JSON.stringify({ date, volumeMl }),
  });
}

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
  readonly items: NutritionItem[];
}

/** Daily macro targets. */
export interface NutritionTargets {
  readonly calories: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

/** A seeded nutrition day: targets plus its ordered meals. */
export interface NutritionDay {
  readonly type: string;
  readonly targets: NutritionTargets;
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

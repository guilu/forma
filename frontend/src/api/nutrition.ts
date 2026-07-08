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

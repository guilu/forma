/**
 * What may stand in for a food (V47), on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>Read by source food rather than as a whole list: "qué puedo comer en vez de
 * esto" is the only question anybody asks of this, and answering it one food at
 * a time keeps the arithmetic proportional to the question.
 *
 * <p>The grams are computed from today's catalog on every read, never stored.
 * Two calls a month apart may legitimately disagree if somebody corrected a
 * food in between — that is the design working, not a bug.
 */
import { apiClient, type ApiClient } from './client';

const PATH = '/api/v1/food-equivalences';

/** Which nutrient the swap holds equal. Closed for good: there are three macros and calories. */
export type EquivalenceBasis = 'CALORIES' | 'PROTEIN' | 'CARBS' | 'FAT';

export interface FoodEquivalence {
  readonly id: string;
  readonly sourceFoodId: string;
  readonly targetFoodId: string;
  /** The replacing food's name, so a screen need not look it up again. */
  readonly targetName: string;
  readonly basis: EquivalenceBasis;
  /** The portion of the source being talked about — the one figure a person picks. */
  readonly sourceReferenceG: number;
  /** How many grams of the target carry as much of the chosen nutrient. Computed. */
  readonly targetReferenceG: number;
  /**
   * How far each macro drifts. Absent for the nutrient being held equal — zero by
   * construction — and for one the source portion carries none of, since a
   * percentage of nothing is not a number.
   */
  readonly caloriesDeviationPct?: number;
  readonly proteinDeviationPct?: number;
  readonly carbsDeviationPct?: number;
  readonly fatDeviationPct?: number;
  readonly maxMacroDeviationPct?: number;
  /** Whether the collateral drift is worth mentioning. Never a reason to refuse the swap. */
  readonly exceedsTolerance: boolean;
  readonly notes?: string;
}

/** What a substitution looks like before it exists. No grams: those are the answer, not the ask. */
export interface NewFoodEquivalence {
  readonly sourceFoodId: string;
  readonly targetFoodId: string;
  readonly basis: EquivalenceBasis;
  readonly sourceReferenceG: number;
  readonly maxMacroDeviationPct?: number;
  readonly notes?: string;
}

/**
 * What may replace this food, with the grams worked out. Open to any signed-in user.
 *
 * <p>One direction only: that rice may be replaced by potato says nothing about
 * the reverse, and the API does not conjure it.
 */
export function listEquivalences(
  foodId: string,
  client: ApiClient = apiClient,
): Promise<FoodEquivalence[]> {
  return client.request<FoodEquivalence[]>(`${PATH}/${encodeURIComponent(foodId)}`);
}

/**
 * States that one food may stand in for another. Admin only.
 *
 * <p>Rejects with 400 when the swap cannot be worked out — either food carrying
 * none of the nutrient being matched — and with 409 when that pair already has
 * advice on those grounds.
 */
export function createEquivalence(
  equivalence: NewFoodEquivalence,
  client: ApiClient = apiClient,
): Promise<FoodEquivalence> {
  return client.request<FoodEquivalence>(PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(equivalence),
  });
}

/** Removes a substitution. Admin only. */
export function deleteEquivalence(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${PATH}/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

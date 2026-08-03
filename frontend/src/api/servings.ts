/**
 * A food's portions (V49), on the shared {@link apiClient} boundary (ADR-006 —
 * no ad-hoc `fetch`).
 *
 * <p>Nested under the food because a portion has no meaning apart from one:
 * "150 g" is not a thing, "a large banana" is.
 *
 * <p>At most one portion per food is the default — what "one serving" means —
 * and the backend enforces it. Claiming it takes it from whichever portion held
 * it, so a screen never has to unset the old one first.
 */
import { apiClient, type ApiClient } from './client';

const FOODS_PATH = '/api/v1/foods';

export interface FoodServing {
  readonly id: string;
  readonly foodId: string;
  /** Absent for the plain portion a food starts with, before sizes were distinguished. */
  readonly name?: string;
  readonly grams: number;
  readonly isDefault: boolean;
  readonly sortOrder: number;
}

/** What a portion looks like before it exists: no id, and the food is in the path. */
export interface NewFoodServing {
  readonly name?: string;
  readonly grams: number;
  readonly isDefault: boolean;
  readonly sortOrder: number;
}

const servingsPath = (foodId: string) => `${FOODS_PATH}/${encodeURIComponent(foodId)}/servings`;

/** Every portion of the food, default first. Open to any signed-in user. */
export function listServings(
  foodId: string,
  client: ApiClient = apiClient,
): Promise<FoodServing[]> {
  return client.request<FoodServing[]>(servingsPath(foodId));
}

/** Adds a portion. Admin only; rejects with 409 when the name is taken for that food. */
export function createServing(
  foodId: string,
  serving: NewFoodServing,
  client: ApiClient = apiClient,
): Promise<FoodServing> {
  return client.request<FoodServing>(servingsPath(foodId), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(serving),
  });
}

/** Replaces a portion. Admin only. */
export function updateServing(
  foodId: string,
  id: string,
  serving: NewFoodServing,
  client: ApiClient = apiClient,
): Promise<FoodServing> {
  return client.request<FoodServing>(`${servingsPath(foodId)}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(serving),
  });
}

/** Removes a portion, including the default one. Admin only. */
export function deleteServing(
  foodId: string,
  id: string,
  client: ApiClient = apiClient,
): Promise<void> {
  return client.request<void>(`${servingsPath(foodId)}/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

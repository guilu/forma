/**
 * Food catalog API (FOR-173 read, FOR-190 maintenance), on the shared
 * {@link apiClient} boundary (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>The catalog is global reference data: one row per food, macros per 100 g,
 * shared by every account. Reading is open to any signed-in user; creating,
 * updating and deleting require the admin role and are rejected server-side
 * with a 403 whatever the UI offers.
 */
import { apiClient, type ApiClient } from './client';

const FOODS_PATH = '/api/v1/foods';

/** The closed set the backend stores; the UI renders its own labels. */
export type FoodCategory = 'CARBOHIDRATO' | 'PROTEINA' | 'FRUTA' | 'VERDURA' | 'GRASA' | 'LACTEO';

/**
 * A catalog food. Every macro is per 100 g; `servingSizeG` is the suggested
 * portion the ration figures are computed from.
 *
 * <p>The optional nutrients are optional in the data, not just in the type:
 * `undefined` means nobody has looked that value up, never zero.
 */
export interface CatalogFood {
  readonly id: string;
  readonly name: string;
  readonly servingSizeG?: number;
  readonly kcal: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
  readonly fiberG?: number;
  readonly sugarsG?: number;
  readonly sodiumMg?: number;
  readonly saturatedFatG?: number;
  readonly category?: FoodCategory;
}

/** Lists the whole catalog, ordered by id. */
export function listFoods(client: ApiClient = apiClient): Promise<CatalogFood[]> {
  return client.request<CatalogFood[]>(FOODS_PATH);
}

/** Adds a food. Admin only; rejects with 409 when the id is taken. */
export function createFood(food: CatalogFood, client: ApiClient = apiClient): Promise<CatalogFood> {
  return client.request<CatalogFood>(FOODS_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(food),
  });
}

/**
 * Replaces a food. Admin only. The id in the path wins: the backend ignores any
 * id in the body, because renaming one would orphan every shopping product
 * pointing at it.
 */
export function updateFood(
  id: string,
  food: CatalogFood,
  client: ApiClient = apiClient,
): Promise<CatalogFood> {
  return client.request<CatalogFood>(`${FOODS_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(food),
  });
}

/** Removes a food. Admin only; rejects when a shopping product still links to it. */
export function deleteFood(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${FOODS_PATH}/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

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

/**
 * The three macronutrients. Unlike a food group, this set is closed for good:
 * there are three because there are three, not because a curator drew the line.
 */
export type PrimaryMacro = 'PROTEIN' | 'CARBS' | 'FAT';

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
  /**
   * The food group this food is filed under, or absent when nobody has
   * classified it. A `food_group` row id — the set is data since V43, so this is
   * a plain string and not a union the bundle would have to be redeployed to
   * widen.
   */
  readonly foodGroupId?: string;
  /**
   * Which macronutrient the food is mostly made of, by calories. Defaulted from
   * the macros when a write omits it, then editable — the arithmetic proposes
   * and a curator may disagree. Absent when the food's own numbers decide
   * nothing: water has no dominant macro.
   */
  readonly primaryMacro?: PrimaryMacro;
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

/**
 * A shop's own aisles (V46), on the shared {@link apiClient} boundary (ADR-006 —
 * no ad-hoc `fetch`).
 *
 * <p>Not to be confused with `ShoppingCategory`, which is one of OUR six aisles
 * and is what the shopping list groups by. This is what the SHOP calls its
 * shelves, copied verbatim and never mapped onto ours automatically.
 *
 * <p>Flat with a `parentId` and a `level` rather than nested: a client that wants
 * a tree can build one in a pass, and one that only wants to indent a list
 * already has what it needs.
 */
import { apiClient, type ApiClient } from './client';

const STORES_PATH = '/api/v1/stores';

export interface StoreCategory {
  readonly id: string;
  readonly storeId: string;
  /** The aisle above, or absent at the top. */
  readonly parentId?: string;
  /** The shop's own id for the aisle — the identity, since names repeat across a tree. */
  readonly externalId: string;
  readonly name: string;
  readonly slug: string;
  /** How deep it sits, 0 at the top. Stored so a screen can indent without walking the chain. */
  readonly level: number;
  readonly sortOrder: number;
}

/**
 * One shop's aisles, parents before children. Open to any signed-in user.
 *
 * <p>Empty until somebody syncs the shop, and permanently empty for a chain with
 * no catalogue behind it — `OTRAS` has none by definition.
 */
export function listStoreCategories(
  storeId: string,
  client: ApiClient = apiClient,
): Promise<StoreCategory[]> {
  return client.request<StoreCategory[]>(
    `${STORES_PATH}/${encodeURIComponent(storeId)}/categories`,
  );
}

/**
 * Re-reads a shop's aisles from the shop itself. Admin only.
 *
 * <p>Rejects with 404 for a chain with no catalogue behind it, which is a real
 * answer rather than a failure: `OTRAS` is where things bought at a market stall
 * go, and there is nothing to ask.
 */
export function syncStoreCategories(
  storeId: string,
  client: ApiClient = apiClient,
): Promise<StoreCategory[]> {
  return client.request<StoreCategory[]>(
    `${STORES_PATH}/${encodeURIComponent(storeId)}/categories/sync`,
    { method: 'POST' },
  );
}

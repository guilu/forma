/**
 * Store product catalog API (FOR-191), on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>Global reference data, one row per product per chain: what can be bought,
 * where, in what package and for how much. Distinct from the shopping list API,
 * which is each account's own copy of what they are buying this week. Reading is
 * open to any signed-in user; the writes require the admin role and are rejected
 * server-side with a 403 whatever the UI offers.
 */
import { apiClient, type ApiClient } from './client';

const STORE_PRODUCTS_PATH = '/api/v1/store-products';

/** The chains the catalog covers. Adding one is a backend enum value first. */
export type Store = 'MERCADONA' | 'CARREFOUR';

/** Grocery aisle, shared with the shopping list's own grouping. */
export type ShoppingCategory =
  | 'FRUTAS_Y_VERDURAS'
  | 'PROTEINAS'
  | 'LACTEOS_Y_HUEVOS'
  | 'CEREALES_Y_LEGUMBRES'
  | 'GRASAS_Y_ACEITES'
  | 'OTROS';

/**
 * A purchasable product.
 *
 * <p>`priceEur` is the price of the package named by `packageSize` — the
 * product's own price, not what a week of it costs. `foodId` links to the food
 * catalog and is absent for anything nobody has matched to a food yet.
 */
export interface StoreProduct {
  readonly id: string;
  readonly store: Store;
  readonly name: string;
  readonly foodId?: string;
  readonly packageSize?: string;
  readonly priceEur?: number;
  readonly url?: string;
  readonly category: ShoppingCategory;
  readonly notes?: string;
}

/** Lists the catalog, narrowed to one chain when `store` is given. */
export function listStoreProducts(
  store?: Store,
  client: ApiClient = apiClient,
): Promise<StoreProduct[]> {
  const query = store ? `?store=${encodeURIComponent(store)}` : '';
  return client.request<StoreProduct[]>(`${STORE_PRODUCTS_PATH}${query}`);
}

/** Adds a product. Admin only; rejects with 409 when the id is taken. */
export function createStoreProduct(
  product: StoreProduct,
  client: ApiClient = apiClient,
): Promise<StoreProduct> {
  return client.request<StoreProduct>(STORE_PRODUCTS_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(product),
  });
}

/**
 * Replaces a product. Admin only. The id in the path wins: the backend ignores
 * any id in the body so an edit can never rename a row out from under a link.
 */
export function updateStoreProduct(
  id: string,
  product: StoreProduct,
  client: ApiClient = apiClient,
): Promise<StoreProduct> {
  return client.request<StoreProduct>(`${STORE_PRODUCTS_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(product),
  });
}

/** Removes a product. Admin only. */
export function deleteStoreProduct(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${STORE_PRODUCTS_PATH}/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

/**
 * A product as the store's own catalogue describes it, before it is ours.
 *
 * <p>Not a {@link StoreProduct}: it carries no `foodId` and no aisle of ours,
 * because those are the two things the shop cannot tell us and an admin decides
 * on import. `storeCategory` is the shop's own shelf name, offered as a hint.
 */
export interface StoreSuggestion {
  readonly externalId: string;
  readonly name: string;
  readonly packaging?: string;
  readonly priceEur?: number;
  readonly url?: string;
  readonly ean?: string;
  readonly storeCategory?: string;
}

/**
 * Products from `store` that look like the food at `foodId`, best first. Admin
 * only. Rejects with 404 for an unknown food or a chain with no source, and with
 * 502 when the shop itself cannot be reached — the screen tells those apart.
 */
export function listStoreSuggestions(
  foodId: string,
  store: Store,
  client: ApiClient = apiClient,
): Promise<StoreSuggestion[]> {
  const query = `?foodId=${encodeURIComponent(foodId)}&store=${encodeURIComponent(store)}`;
  return client.request<StoreSuggestion[]>(`${STORE_PRODUCTS_PATH}/suggestions${query}`);
}

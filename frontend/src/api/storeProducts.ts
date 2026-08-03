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
export type Store = 'MERCADONA' | 'CARREFOUR' | 'OTRAS';

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
  /** The shop's own id, present only on imported rows — what makes a refresh possible. */
  readonly externalId?: string;
  readonly imageUrl?: string;
  /**
   * Which shelf of its OWN shop the product sits on, as one of our
   * `store_category` rows. Read-only here: the server resolves it from the id
   * the shop reported, so nothing on a form writes it.
   */
  readonly storeCategoryId?: string;
  /**
   * The SHOP's own id for that shelf, echoed back from the suggestion the same
   * way `externalId` is. Sent on create so the server can resolve it; absent for
   * a product typed by hand, which never came off a shelf.
   */
  readonly storeCategoryExternalId?: string;
  /** Barcode, when the shop publishes one. The only key that means the same in every shop. */
  readonly ean?: string;
  /**
   * How much the package holds, with the unit the shop stated — 5 and `'l'`, not 5000 g.
   * Deliberately unconverted: five litres is not five of anything weighable without a density.
   */
  readonly packageAmount?: number;
  readonly packageUnit?: string;
  /**
   * Whether the shop still lists it. Always present on a product read from the
   * API — the column is NOT NULL — and absent on one being drafted to send,
   * because the server decides it and no form offers it.
   */
  readonly available?: boolean;
  /** When the shop was last asked. Absent for a product nobody imported. */
  readonly lastSyncedAt?: string;
  /** Who makes it. Never filled by an import — no shop publishes it apart from the name. */
  readonly brand?: string;
}

/** Lists the catalog, narrowed to one chain when `store` is given. */
export function listStoreProducts(
  store?: Store,
  client: ApiClient = apiClient,
): Promise<StoreProduct[]> {
  const query = store ? `?store=${encodeURIComponent(store)}` : '';
  return client.request<StoreProduct[]>(`${STORE_PRODUCTS_PATH}${query}`);
}

/**
 * Products from `store` whose name contains everything in `query`, best first.
 * Admin only.
 *
 * <p>The other entry point starts from a food in our catalog; this one starts
 * from what somebody types, for the products our own catalog cannot name.
 */
export function searchStoreProducts(
  query: string,
  store: Store,
  client: ApiClient = apiClient,
): Promise<StoreSuggestion[]> {
  const params = `?q=${encodeURIComponent(query)}&store=${encodeURIComponent(store)}`;
  return client.request<StoreSuggestion[]>(`${STORE_PRODUCTS_PATH}/suggestions${params}`);
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

/**
 * Re-reads an imported product from its shop and stores what the shop owns:
 * name, package, price, link and photo. The food link, the aisle and the notes
 * stay as they are — those are ours. Admin only.
 *
 * <p>Rejects with 409 for a product that was never imported, 404 when the shop
 * no longer lists it, and 502 when the shop cannot be reached.
 */
export function refreshStoreProduct(
  id: string,
  client: ApiClient = apiClient,
): Promise<StoreProduct> {
  return client.request<StoreProduct>(`${STORE_PRODUCTS_PATH}/${encodeURIComponent(id)}/refresh`, {
    method: 'POST',
  });
}

/**
 * The product photo the page at `url` advertises, if any. Admin only.
 *
 * <p>Best effort: most shops publish an Open Graph image, some publish none, and
 * then the field is filled by hand. Resolves with `undefined` when the page was
 * read and said nothing — different from rejecting, which means it could not be
 * read at all.
 */
export function fetchLinkImage(
  url: string,
  client: ApiClient = apiClient,
): Promise<string | undefined> {
  return client
    .request<{ imageUrl: string | null }>(
      `${STORE_PRODUCTS_PATH}/link-image?url=${encodeURIComponent(url)}`,
    )
    .then((response) => response.imageUrl ?? undefined);
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
  /** The shop's own id for that shelf — the name is to read, this is to link by. */
  readonly storeCategoryExternalId?: string;
  readonly imageUrl?: string;
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

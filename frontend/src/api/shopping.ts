/**
 * Shopping list API calls (FOR-39), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model and toggles
 * checked state; it owns no cost/budget rules.
 */
import { apiClient, type ApiClient } from './client';

/** One checklist item. */
export interface ShoppingItem {
  readonly id: string;
  /** The FOR-36 product id (FOR-106) — resolves product edits by id, not by name. */
  readonly productId: string;
  readonly productName: string;
  /** Resolved product category (FOR-106); `OTROS` when the product has none. */
  readonly category: string;
  readonly quantity: number;
  /** Unit of measure for {@link quantity} (FOR-108, e.g. `UD`, `G`, `KG`, `L`, `PAQUETE`). */
  readonly unit: string;
  /**
   * Number of servings this line represents (FOR-108); `null` when the resolved product isn't
   * linked to a nutrition food — never fabricated for non-food items.
   */
  readonly servings: number | null;
  readonly estimatedCostEur: number;
  readonly checked: boolean;
  /**
   * Provider link-out/add-to-cart URL (FOR-108/FOR-109); absent/`null` when
   * the resolved product has none — the link-out control is omitted for
   * that row, never shown disabled (FOR-118). Optional (not required) so
   * fixtures predating this field across the app don't need updating.
   */
  readonly productUrl?: string | null;
}

/** Weekly + monthly budget. */
export interface ShoppingBudget {
  readonly weeklyEur: number;
  readonly monthlyEur: number;
}

/** The weekly shopping list with its budget. */
export interface ShoppingList {
  readonly weekStartDate: string;
  readonly status: string;
  readonly items: ShoppingItem[];
  readonly budget: ShoppingBudget;
  /** When this list was generated/created (FOR-108); backfilled for pre-migration lists. */
  readonly generatedAt: string;
}

/** The updated item returned by the check toggle. */
export interface CheckedResult {
  readonly id: string;
  readonly checked: boolean;
}

/**
 * The updated item returned by a quantity edit (FOR-109): the backend
 * recalculates {@link QuantityResult.estimatedCostEur} — the frontend never
 * computes it client-side (ADR-006).
 */
export interface QuantityResult {
  readonly id: string;
  readonly quantity: number;
  readonly estimatedCostEur: number;
  readonly unit: string;
}

/**
 * A shopping product (FOR-36) — the price/URL reference the checklist items
 * point to. `GET /api/v1/shopping/list` items carry {@link ShoppingItem.productId}
 * directly (FOR-106), so callers resolve the product by id, not by name.
 */
export interface ShoppingProduct {
  readonly id: string;
  readonly name: string;
  readonly url?: string;
  readonly packageSize?: string;
  readonly estimatedPriceEur: number;
  readonly pricePerUnitEur?: number;
  readonly linkedFoodItemId?: string;
  readonly lastCheckedAt?: string;
  readonly notes?: string;
}

/** Fields accepted by create/update (FOR-36 `ShoppingProductRequest` mirror). */
export interface ShoppingProductInput {
  readonly name: string;
  readonly url?: string;
  readonly packageSize?: string;
  readonly estimatedPriceEur: number;
  readonly pricePerUnitEur?: number;
  readonly linkedFoodItemId?: string;
  readonly notes?: string;
}

/** Fetches the current week's shopping list + budget. */
export function getShoppingList(client: ApiClient = apiClient): Promise<ShoppingList> {
  return client.request<ShoppingList>('/api/v1/shopping/list');
}

/** Sets an item's checked state. */
export function setItemChecked(
  itemId: string,
  checked: boolean,
  client: ApiClient = apiClient,
): Promise<CheckedResult> {
  return client.request<CheckedResult>(
    `/api/v1/shopping/list/items/${encodeURIComponent(itemId)}/checked`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ checked }),
    },
  );
}

/**
 * Rebuilds the active list from the current product catalog (FOR-109);
 * resets checked state and stamps a new {@link ShoppingList.generatedAt}.
 * Returns the full rebuilt list, since every item may change.
 */
export function regenerateShoppingList(client: ApiClient = apiClient): Promise<ShoppingList> {
  return client.request<ShoppingList>('/api/v1/shopping/list/regenerate', { method: 'POST' });
}

/** Edits an item's quantity (FOR-109); the backend recalculates {@link QuantityResult.estimatedCostEur}. */
export function updateItemQuantity(
  itemId: string,
  quantity: number,
  client: ApiClient = apiClient,
): Promise<QuantityResult> {
  return client.request<QuantityResult>(
    `/api/v1/shopping/list/items/${encodeURIComponent(itemId)}`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ quantity }),
    },
  );
}

/** Lists shopping products (FOR-36), ordered by name. */
export function listShoppingProducts(client: ApiClient = apiClient): Promise<ShoppingProduct[]> {
  return client.request<ShoppingProduct[]>('/api/v1/shopping/products');
}

/** Updates a shopping product's fields (FOR-36) — used for the price/URL edit entry point. */
export function updateShoppingProduct(
  id: string,
  input: ShoppingProductInput,
  client: ApiClient = apiClient,
): Promise<ShoppingProduct> {
  return client.request<ShoppingProduct>(`/api/v1/shopping/products/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
}

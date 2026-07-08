/**
 * Shopping list API calls (FOR-39), built on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`). The frontend renders the read model and toggles
 * checked state; it owns no cost/budget rules.
 */
import { apiClient, type ApiClient } from './client';

/** One checklist item. */
export interface ShoppingItem {
  readonly id: string;
  readonly productName: string;
  readonly quantity: number;
  readonly estimatedCostEur: number;
  readonly checked: boolean;
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
}

/** The updated item returned by the check toggle. */
export interface CheckedResult {
  readonly id: string;
  readonly checked: boolean;
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

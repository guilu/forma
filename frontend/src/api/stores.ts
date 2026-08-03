/**
 * The supermarket chains (V45), on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>Which chains exist stopped being something a client could hardcode the
 * moment it became a table. Adding Lidl is an insert, and a bundle that carries
 * its own list would go on offering three options after it.
 *
 * <p>Read-only here. A chain needs a name and a place in the order, and the
 * screen that would ask for those does not exist yet — offering a half-filled
 * row would be worse than not offering one.
 */
import { apiClient, type ApiClient } from './client';

const STORES_PATH = '/api/v1/stores';

export interface Store {
  /** The stored token every product points at. Never editable. */
  readonly id: string;
  readonly name: string;
  /** The chain's mark. Nothing renders it yet. */
  readonly logoUrl?: string;
  /** The public storefront. `OTRAS` has none by definition. */
  readonly website?: string;
  readonly sortOrder: number;
  readonly enabled: boolean;
}

/** Every chain, in the order they should be shown. Open to any signed-in user. */
export function listStores(client: ApiClient = apiClient): Promise<Store[]> {
  return client.request<Store[]>(STORES_PATH);
}

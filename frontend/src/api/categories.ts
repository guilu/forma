/**
 * Category labels and icons (FOR-197), on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>How a category reads is data; which categories exist is not. The set is
 * closed in the backend's enums and in the database's own CHECK constraints, so
 * there is no create and no delete here — only a rename and a change of glyph.
 *
 * <p>Two vocabularies live side by side: `FOOD` files an ingredient by what it is
 * made of, `SHOPPING` files a product by which aisle it sits in. "Proteína" is in
 * both and means a different thing in each.
 */
import { apiClient, type ApiClient } from './client';

const CATEGORIES_PATH = '/api/v1/categories';

export type CategoryScope = 'FOOD' | 'SHOPPING';

export interface CategoryDisplay {
  readonly scope: CategoryScope;
  /** The stored token every row points at. Never editable. */
  readonly code: string;
  readonly label: string;
  readonly icon?: string;
}

/** Every category, or one vocabulary's. Open to any signed-in user. */
export function listCategories(
  scope?: CategoryScope,
  client: ApiClient = apiClient,
): Promise<CategoryDisplay[]> {
  const query = scope ? `?scope=${encodeURIComponent(scope)}` : '';
  return client.request<CategoryDisplay[]>(`${CATEGORIES_PATH}${query}`);
}

/** Renames a category and/or changes its icon. Admin only. */
export function updateCategory(
  scope: CategoryScope,
  code: string,
  display: { readonly label: string; readonly icon?: string },
  client: ApiClient = apiClient,
): Promise<CategoryDisplay> {
  return client.request<CategoryDisplay>(
    `${CATEGORIES_PATH}/${encodeURIComponent(scope)}/${encodeURIComponent(code)}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(display),
    },
  );
}

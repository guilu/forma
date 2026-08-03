/**
 * Labels a food can carry (V50), on the shared {@link apiClient} boundary
 * (ADR-006 — no ad-hoc `fetch`).
 *
 * <p>Two calls that look like one thing and are not: the vocabulary is what
 * labels exist, and a food's labels are which of them it carries. The first is a
 * decision about what the catalog can say; the second is a claim about a food.
 *
 * <p>There is no create here. The vocabulary is not something a food form should
 * be able to grow by accident — that is how "Vegano" and "vegano" end up side by
 * side.
 */
import { apiClient, type ApiClient } from './client';

const TAGS_PATH = '/api/v1/tags';
const FOODS_PATH = '/api/v1/foods';

export interface Tag {
  /** The stored token; never editable. */
  readonly id: string;
  readonly name: string;
  /** Grouped by kind rather than alphabetically: what it is, how it is kept, when it suits. */
  readonly sortOrder: number;
}

/** Every label there is. Open to any signed-in user. */
export function listTags(client: ApiClient = apiClient): Promise<Tag[]> {
  return client.request<Tag[]>(TAGS_PATH);
}

/** The labels a food carries, in the vocabulary's order. Open to any signed-in user. */
export function listFoodTags(foodId: string, client: ApiClient = apiClient): Promise<Tag[]> {
  return client.request<Tag[]>(`${FOODS_PATH}/${encodeURIComponent(foodId)}/tags`);
}

/**
 * Sets a food's labels to exactly these. Admin only.
 *
 * <p>Replaces rather than adds: the form shows every label at once, so what it
 * leaves out is what somebody unticked. An empty list clears them, which has to
 * be possible — untickng the last checkbox is a thing people do.
 */
export function setFoodTags(
  foodId: string,
  tagIds: readonly string[],
  client: ApiClient = apiClient,
): Promise<Tag[]> {
  return client.request<Tag[]>(`${FOODS_PATH}/${encodeURIComponent(foodId)}/tags`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tagIds }),
  });
}

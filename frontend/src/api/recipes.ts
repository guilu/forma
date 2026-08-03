/**
 * Recipes (V52), on the shared {@link apiClient} boundary (ADR-006 — no ad-hoc
 * `fetch`).
 *
 * <p>A recipe carries no nutrition of its own: `total` and `perServing` are the
 * sum over its ingredients of what the catalog says, computed on every read. Two
 * calls a month apart may legitimately disagree if somebody corrected a food in
 * between — that is the design working.
 *
 * <p>Nothing here sends them. They are what the ingredients add up to, and a
 * body offering them would be offering numbers the server has to ignore.
 */
import { apiClient, type ApiClient } from './client';

const RECIPES_PATH = '/api/v1/recipes';

export interface RecipeIngredient {
  readonly foodId: string;
  /** How much goes in, as the catalog records that food — dry rice is listed dry. */
  readonly grams: number;
}

export interface RecipeTotals {
  readonly calories: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

export interface Recipe {
  readonly id: string;
  readonly name: string;
  /** How many portions the whole thing makes. A stew for four is not a meal for one. */
  readonly servings: number;
  readonly notes?: string;
  readonly ingredients: readonly RecipeIngredient[];
  /** The whole dish, summed from the catalog at the moment of the request. */
  readonly total: RecipeTotals;
  /** The same divided by `servings` — the figure anybody eating it wants. */
  readonly perServing: RecipeTotals;
  /** Ingredients whose food has gone. Should always be empty; a foreign key protects them. */
  readonly unknownFoodIds: readonly string[];
}

/** What a recipe looks like before it exists. No totals: those are the answer, not the ask. */
export interface NewRecipe {
  readonly id: string;
  readonly name: string;
  readonly servings: number;
  readonly notes?: string;
  readonly ingredients: readonly RecipeIngredient[];
}

/** Every recipe, each with what it works out to. Open to any signed-in user. */
export function listRecipes(client: ApiClient = apiClient): Promise<Recipe[]> {
  return client.request<Recipe[]>(RECIPES_PATH);
}

/** Adds a recipe. Admin only; rejects with 409 when the id is taken. */
export function createRecipe(recipe: NewRecipe, client: ApiClient = apiClient): Promise<Recipe> {
  return client.request<Recipe>(RECIPES_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(recipe),
  });
}

/** Replaces a recipe. Admin only. The id in the path wins over any in the body. */
export function updateRecipe(
  id: string,
  recipe: NewRecipe,
  client: ApiClient = apiClient,
): Promise<Recipe> {
  return client.request<Recipe>(`${RECIPES_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(recipe),
  });
}

/** Removes a recipe and its ingredients. Admin only. */
export function deleteRecipe(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${RECIPES_PATH}/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

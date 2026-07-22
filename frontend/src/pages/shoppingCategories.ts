/**
 * Category tab and filtering helpers for {@link ShoppingPage} (FOR-111).
 *
 * <p>Split out of `ShoppingPage.tsx` so the pure tab-building/filtering logic
 * is directly unit-testable and the component file keeps exporting only the
 * component (`react-refresh/only-export-components`), matching the existing
 * `onboardingStorage.ts`/`profileData.ts` convention of a sibling `.ts`
 * module for page-local, non-component logic.
 */
import type { ShoppingItem } from '../api/shopping';
import type { IconName } from '../components/Icon';

/** Sentinel tab key for "show every category". */
export const ALL_CATEGORIES = 'ALL';

/**
 * Human-readable labels for the FOR-106 `ShoppingCategory` enum, in the
 * backend's declared order. Tabs render "Todas" first, then categories in
 * this order (spec.md open question resolved this way) — display-only
 * mapping of a closed enum, not category inference (ADR-006).
 */
const CATEGORY_LABELS: ReadonlyArray<readonly [string, string]> = [
  ['FRUTAS_Y_VERDURAS', 'Frutas y verduras'],
  ['PROTEINAS', 'Proteínas'],
  ['LACTEOS_Y_HUEVOS', 'Lácteos y huevos'],
  ['CEREALES_Y_LEGUMBRES', 'Cereales y legumbres'],
  ['GRASAS_Y_ACEITES', 'Grasas y aceites'],
  ['OTROS', 'Otros'],
];

const CATEGORY_LABEL_MAP = new Map(CATEGORY_LABELS);
const FALLBACK_CATEGORY = 'OTROS';

/**
 * Normalizes an item's category to a known key, falling back to "Otros" for
 * missing/unrecognized values — mirrors the FOR-106 backend default
 * defensively (edge case: item with category `OTROS`/null groups under
 * "Otros", not hidden).
 */
function normalizeCategory(category: string | null | undefined): string {
  return category && CATEGORY_LABEL_MAP.has(category) ? category : FALLBACK_CATEGORY;
}

/** Display label for a tab key, including the {@link ALL_CATEGORIES} sentinel. */
export function categoryLabel(categoryKey: string): string {
  if (categoryKey === ALL_CATEGORIES) {
    return 'Todas';
  }
  return (
    CATEGORY_LABEL_MAP.get(categoryKey) ?? (CATEGORY_LABEL_MAP.get(FALLBACK_CATEGORY) as string)
  );
}

/**
 * Builds the tab list: "Todas" first (with the total item count), then one
 * tab per distinct category actually present in `items`, in canonical order.
 * A category absent from the current list gets no tab (spec.md: "build the
 * tab set from the distinct categories present in the current list").
 */
export function buildCategoryTabs(
  items: readonly ShoppingItem[],
): ReadonlyArray<{ readonly key: string; readonly label: string }> {
  const present = new Set(items.map((item) => normalizeCategory(item.category)));
  const categoryTabs = CATEGORY_LABELS.filter(([key]) => present.has(key)).map(([key, label]) => ({
    key,
    label: `${label} (${items.filter((item) => normalizeCategory(item.category) === key).length})`,
  }));
  return [{ key: ALL_CATEGORIES, label: `Todas (${items.length})` }, ...categoryTabs];
}

/**
 * Filters items to one category. `ALL_CATEGORIES` returns everything; any
 * other key with no matches (e.g. a stale tab selection after a list
 * refresh) returns an empty array rather than throwing — the caller renders
 * a scoped empty state for that case (tests.md edge case).
 */
export function filterItemsByCategory(
  items: readonly ShoppingItem[],
  categoryKey: string,
): readonly ShoppingItem[] {
  if (categoryKey === ALL_CATEGORIES) {
    return items;
  }
  return items.filter((item) => normalizeCategory(item.category) === categoryKey);
}

/**
 * Groups items under their category, in the canonical `CATEGORY_LABELS` order,
 * for the FOR-164 grouped table (the mockup renders a category header row —
 * icon + uppercase label — before each cluster of item rows). Only categories
 * actually present in `items` produce a group; each group carries its display
 * label so the caller doesn't re-resolve it.
 */
export function groupItemsByCategory(items: readonly ShoppingItem[]): ReadonlyArray<{
  readonly key: string;
  readonly label: string;
  readonly items: readonly ShoppingItem[];
}> {
  return CATEGORY_LABELS.map(([key, label]) => ({
    key,
    label,
    items: items.filter((item) => normalizeCategory(item.category) === key),
  })).filter((group) => group.items.length > 0);
}

/**
 * Decorative glyph for a category header (FOR-164). Maps the closed
 * `ShoppingCategory` enum onto the shell's existing {@link IconName} set — no
 * new icons, no per-product emoji (there's no backing data for that); food
 * categories share the nutrition glyph, "Otros" reuses the shopping basket.
 */
const CATEGORY_ICONS: ReadonlyMap<string, IconName> = new Map([
  ['FRUTAS_Y_VERDURAS', 'nutrition'],
  ['PROTEINAS', 'activity'],
  ['LACTEOS_Y_HUEVOS', 'nutrition'],
  ['CEREALES_Y_LEGUMBRES', 'nutrition'],
  ['GRASAS_Y_ACEITES', 'nutrition'],
  ['OTROS', 'shopping'],
]);

/** Icon for a category header row; falls back to the shopping glyph. */
export function categoryIcon(categoryKey: string): IconName {
  return CATEGORY_ICONS.get(categoryKey) ?? 'shopping';
}

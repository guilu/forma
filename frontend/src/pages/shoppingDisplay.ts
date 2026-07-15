/**
 * Item-display formatting helpers for {@link ShoppingPage} (FOR-117): a
 * display label for `ShoppingItem.unit` and a consistent formatter for
 * `ShoppingList.generatedAt`. Split out of the component file so this pure
 * logic is directly unit-testable and the component file keeps exporting
 * only the component (`react-refresh/only-export-components`), matching the
 * sibling `shoppingCategories.ts` convention (FOR-111).
 */

/**
 * Human-readable labels for the FOR-108 `ShoppingUnit` enum (backend:
 * `UD`, `G`, `KG`, `L`, `PAQUETE`), mirroring the `CATEGORY_LABELS`
 * display-only mapping pattern in `shoppingCategories.ts`. A closed enum
 * label lookup, not unit conversion — the numeric quantity itself is
 * rendered exactly as the backend returns it (ADR-006).
 */
const UNIT_LABELS: ReadonlyMap<string, string> = new Map([
  ['UD', 'ud'],
  ['G', 'g'],
  ['KG', 'kg'],
  ['L', 'l'],
  ['PAQUETE', 'paquete'],
]);

/**
 * Display label for a `unit` value. Falls back to the raw value when the
 * frontend doesn't have a specific label for it yet (spec.md edge case: an
 * unrecognized/future unit renders as-is rather than crashing or hiding it).
 */
export function unitLabel(unit: string): string {
  return UNIT_LABELS.get(unit) ?? unit;
}

/**
 * Same day/month/hour/minute pattern as `IntegrationsSection`'s
 * `lastSyncFormatter` — reused here for `ShoppingList.generatedAt` so the
 * "Generada" tile matches how the app formats other timestamps.
 */
const generatedAtFormatter = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});

/** Formats `ShoppingList.generatedAt` for the "Generada" tile. */
export function formatGeneratedAt(iso: string): string {
  return generatedAtFormatter.format(new Date(iso));
}

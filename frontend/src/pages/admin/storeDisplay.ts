import type { ShoppingCategory, Store } from '../../api/storeProducts';

/**
 * Display labels for the store catalog's stored tokens (FOR-191).
 *
 * <p>The backend stores accent-stripped uppercase identifiers so the values are
 * stable across languages and migrations; the screen renders these.
 */
export const STORE_LABELS: Record<Store, string> = {
  MERCADONA: 'Mercadona',
  CARREFOUR: 'Carrefour',
};

export const STORE_OPTIONS = Object.keys(STORE_LABELS) as Store[];

/** The grocery aisles, shared with the shopping list's own grouping (V7). */
export const SHOPPING_CATEGORY_LABELS: Record<ShoppingCategory, string> = {
  FRUTAS_Y_VERDURAS: 'Frutas y verduras',
  PROTEINAS: 'Proteínas',
  LACTEOS_Y_HUEVOS: 'Lácteos y huevos',
  CEREALES_Y_LEGUMBRES: 'Cereales y legumbres',
  GRASAS_Y_ACEITES: 'Grasas y aceites',
  OTROS: 'Otros',
};

export const SHOPPING_CATEGORY_OPTIONS = Object.keys(
  SHOPPING_CATEGORY_LABELS,
) as ShoppingCategory[];

/** A glyph per aisle, so a phone row is scannable by shape before it is read. */
const CATEGORY_GLYPHS: Record<ShoppingCategory, string> = {
  FRUTAS_Y_VERDURAS: '🥦',
  PROTEINAS: '🍗',
  LACTEOS_Y_HUEVOS: '🥛',
  CEREALES_Y_LEGUMBRES: '🌾',
  GRASAS_Y_ACEITES: '🫒',
  OTROS: '🛒',
};

export const shoppingCategoryGlyph = (category: ShoppingCategory) => CATEGORY_GLYPHS[category];

/**
 * Prices are money: two decimals, comma separator, euro sign — never a bare
 * number. An absent price stays absent; a product nobody has priced is a real
 * state and a 0 € would be a claim about it.
 */
export const priceLabel = (priceEur?: number) =>
  priceEur === undefined || priceEur === null
    ? '—'
    : `${priceEur.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €`;

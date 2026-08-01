import type { FoodCategory } from '../../api/foods';

/**
 * Display labels for the stored category tokens (FOR-190).
 *
 * <p>The backend stores accent-stripped uppercase identifiers so the value is
 * stable; the screen renders these instead, exactly as the source spreadsheet
 * writes them.
 */
export const CATEGORY_LABELS: Record<FoodCategory, string> = {
  CARBOHIDRATO: 'Carbohidrato',
  PROTEINA: 'Proteína',
  FRUTA: 'Fruta',
  VERDURA: 'Verdura',
  GRASA: 'Grasa',
  LACTEO: 'Lácteo',
};

/** The categories a food can be filed under, in the order the sheet lists them. */
export const CATEGORY_OPTIONS = Object.keys(CATEGORY_LABELS) as FoodCategory[];

/**
 * A glyph per category, so a row in the phone list is scannable by shape before
 * it is read.
 *
 * <p>Keyed by category rather than by food id on purpose. An id map would cover
 * the seeded catalog and leave every food an admin creates afterwards blank, and
 * it would put data *about a food* in the frontend bundle, where changing it
 * needs a deploy — in a screen that exists precisely so the catalog can be
 * edited without one. Two carbohydrates sharing a glyph is the price.
 *
 * <p>Decorative: `aria-hidden` at the call site. The category is written out in
 * the row's detail panel, so the glyph never carries information alone.
 */
const CATEGORY_GLYPHS: Record<FoodCategory, string> = {
  CARBOHIDRATO: '🌾',
  PROTEINA: '🍗',
  FRUTA: '🍎',
  VERDURA: '🥦',
  GRASA: '🫒',
  LACTEO: '🥛',
};

/** A neutral plate for a food nobody has classified yet. */
export const categoryGlyph = (category?: FoodCategory) =>
  category ? CATEGORY_GLYPHS[category] : '🍽️';

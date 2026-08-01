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

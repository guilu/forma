package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The initial food catalog (FOR-30): the common foods used by the nutrition plan, with estimated
 * per-100 g nutrition values.
 *
 * <p>Defined in code with stable ids rather than as persisted seed data — no nutrition persistence
 * exists yet and meal items (FOR-31) only need to reference foods by a stable id (spec FOR-30 Open
 * Questions, consistent with the FOR-24 exercise catalog). No migration is introduced. Values are
 * sensible MVP estimates per 100 g, not medical data; product/price data lives in Shopping (FOR-5).
 *
 * <p><b>Key nutrients (FOR-134).</b> Fibre/sugars/sodium/saturated-fat per 100 g are populated only
 * where a reasonably confident general reference value exists (single-ingredient, low
 * brand/preparation variance — e.g. raw oats, raw banana, plain dairy, unseasoned meat's zero
 * carbs). They are left {@code null} wherever the food is too variable to responsibly assert a
 * precise number (e.g. "queso fresco" and "verduras mixtas" are generic,
 * brand/composition-dependent groupings; sodium for cooked staples like rice/potato/meat depends
 * heavily on whether salt was added during preparation, which this catalog does not track) — never
 * fabricated, per AGENTS.md and the FOR-134 spec. This intentionally yields a mix of
 * fully-populated, partially-populated and fully-null entries (see {@code FoodCatalogTest}).
 */
public final class FoodCatalog {

  private static final List<FoodItem> FOODS =
      List.of(
          // Fully known: matches a standard raw-oats reference (fibre/sugars/sodium/sat-fat).
          new FoodItem("oats", "Avena", 389, 16.9, 66.3, 6.9, 60, 10.6, 0.0, 2.0, 1.2),
          // Partial: eggs have no fibre (confident); sodium/sat-fat are well-established for whole
          // eggs; sugars is left null (too small/uncertain to assert precisely).
          new FoodItem("eggs", "Huevos", 155, 13.0, 1.1, 11.0, 100, 0.0, null, 124.0, 3.3),
          // Fully known: plain whole-milk yogurt's carbs are essentially all lactose (sugars).
          new FoodItem("yogurt", "Yogur natural", 61, 3.5, 4.7, 3.3, 125, 0.0, 4.7, 36.0, 2.0),
          // Fully null: "queso fresco" varies too much by brand/region to assert a precise value.
          new FoodItem("fresh-cheese", "Queso fresco", 98, 11.0, 3.4, 4.0, 100),
          // Partial: meat has no carbs (fibre/sugars confidently 0); sat-fat is well known for
          // skinless chicken breast; sodium depends on preparation -> left null.
          new FoodItem("chicken", "Pollo (pechuga)", 165, 31.0, 0.0, 3.6, 150, 0.0, 0.0, null, 1.0),
          new FoodItem("turkey", "Pavo (pechuga)", 135, 29.0, 0.0, 1.0, 150, 0.0, 0.0, null, 0.3),
          // Partial: fibre/sugars confidently 0 (fish has no carbs); sat-fat/sodium vary too much
          // across white-fish species/preparation to assert -> left null.
          new FoodItem("fish", "Pescado blanco", 96, 20.0, 0.0, 1.5, 150, 0.0, 0.0, null, null),
          // Partial: cooked white rice has low, fairly consistent fibre/sat-fat and negligible
          // sugar; sodium depends entirely on whether salt was added while cooking -> left null.
          new FoodItem("rice", "Arroz cocido", 130, 2.7, 28.0, 0.3, 150, 0.4, 0.0, null, 0.1),
          // Partial: boiled potato flesh has fairly consistent fibre/sugars/sat-fat; sodium depends
          // on whether salted -> left null.
          new FoodItem("potato", "Patata cocida", 87, 2.0, 20.0, 0.1, 200, 1.8, 0.8, null, 0.0),
          // Fully known: matches a standard raw-banana reference exactly on all four macros, so its
          // companion key-nutrient values are trusted from the same source.
          new FoodItem("banana", "Plátano", 89, 1.1, 22.8, 0.3, 120, 2.6, 12.2, 1.0, 0.1),
          // Fully null: a generic "mixed vegetables" grouping has no single accurate composition.
          new FoodItem("vegetables", "Verduras mixtas", 40, 2.5, 7.0, 0.4, 200),
          // Mostly null: whey protein powder's sugar/sodium/sat-fat vary hugely by brand/flavour;
          // only "no fibre" is a safe general claim for a protein powder.
          new FoodItem(
              "whey-protein", "Proteína whey", 400, 80.0, 8.0, 6.0, 30, 0.0, null, null, null));

  private FoodCatalog() {}

  /** All catalog foods (immutable). */
  public static List<FoodItem> foods() {
    return FOODS;
  }

  /** Finds a food by its stable id. */
  public static Optional<FoodItem> findById(String id) {
    return FOODS.stream().filter(food -> food.id().equals(id)).findFirst();
  }
}

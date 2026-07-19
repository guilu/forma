package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The food catalog (FOR-30, reseeded FOR-152): Diego's 23 real foods from {@code
 * docs/fitness_os.xlsm} sheet <b>Macros</b> (epic FOR-148 "Personalizar FORMA a Diego", slice 4),
 * replacing the original 12 generic demo foods.
 *
 * <p>Defined in code with stable ids rather than as persisted seed data — no nutrition persistence
 * exists yet and meal items (FOR-31) only need to reference foods by a stable id (spec FOR-30 Open
 * Questions, reaffirmed by FOR-152's spec: "keep the in-code approach unless persistence is
 * otherwise needed"). No migration is introduced here. kcal/protein/carbs/fat and the default
 * serving (ración) are copied verbatim from the Macros sheet, in the sheet's own row order.
 *
 * <p><b>Key nutrients (FOR-134).</b> The Macros sheet has no fibre/sugars/sodium/saturated-fat
 * columns at all — so, per AGENTS.md and the FOR-134/FOR-152 "never fabricate" rule, most of these
 * 23 foods carry fully {@code null} key nutrients. A few keep a populated (full or partial) profile
 * only where a stable, brand-independent reference genuinely applies regardless of Diego's own
 * tracked macro numbers: rolled oats (a standard, low-variance category), a raw banana (whose
 * macros match the old catalog's banana reference exactly), whole eggs (fibre confidently zero;
 * sodium/sat-fat are well-established for a standardised product), pure olive oil (a single
 * ingredient with well-known composition), and "0 g carbs/100g -> fibre/sugars confidently 0" for
 * the meats/fish whose sheet row shows exactly zero carbohydrate (pollo, atún, merluza, salmón).
 * Foods whose carbs aren't exactly zero (e.g. pavo at 1 g/100g), or that are branded/composite
 * products (queso fresco batido 0%, yogur proteína, pasta integral, pan integral, frutos secos...),
 * are left fully {@code null} rather than guessing a plausible-looking number.
 */
public final class FoodCatalog {

  private static final List<FoodItem> FOODS =
      List.of(
          // Fully known: rolled oats is a stable, low-variance category; the fibre/sugars/sodium/
          // sat-fat figures mirror the same rolled-oats reference the pre-FOR-152 catalog used.
          new FoodItem("oats", "Copos de avena", 370, 13.0, 60.0, 7.0, 60, 10.6, 0.0, 2.0, 1.2),
          // Mostly null: whey protein powder's sugar/sodium/sat-fat vary hugely by brand/flavour;
          // only "no fibre" is a safe general claim for a protein powder.
          new FoodItem(
              "whey-protein", "Whey proteína", 390, 78.0, 8.0, 6.0, 30, 0.0, null, null, null),
          // Fully known: macros match a standard raw-banana reference exactly (same as the old
          // catalog's banana), so its companion key-nutrient values are trusted from that source.
          new FoodItem("banana", "Plátano", 89, 1.1, 23.0, 0.3, 120, 2.6, 12.2, 1.0, 0.1),
          // Partial: whole eggs have no fibre (confident, animal product); sodium/sat-fat are
          // well-established for a standardised whole egg. Sugars left null (too small/uncertain).
          new FoodItem("eggs", "Huevos", 143, 13.0, 1.0, 10.0, 120, 0.0, null, 124.0, 3.3),
          // Partial: liquid egg white is a pure animal protein (no fibre, confident); sugars/
          // sodium/sat-fat depend on pasteurisation/additives -> left null.
          new FoodItem(
              "egg-whites", "Claras líquidas", 48, 10.5, 0.7, 0.2, 150, 0.0, null, null, null),
          // Fully null: a specific branded product (0% fat fresh cheese) -> never fabricated.
          new FoodItem("fresh-cheese", "Queso fresco batido 0%", 46, 8.5, 3.5, 0.1, 250),
          // Fully null: a branded protein yogurt's sweetener/sodium profile is not given by the
          // sheet.
          new FoodItem("yogurt", "Yogur proteína", 59, 10.0, 4.0, 0.2, 200),
          // Partial: pollo has 0 g carbs/100g -> fibre/sugars confidently 0; sodium and sat-fat
          // are cut/prep-dependent and not given by the sheet -> left null.
          new FoodItem("chicken", "Pechuga pollo", 110, 23.0, 0.0, 2.0, 200, 0.0, 0.0, null, null),
          // Fully null: pavo has 1 g carbs/100g (not confidently 0), so unlike chicken this is not
          // asserted.
          new FoodItem("turkey", "Pavo lonchas/corte", 105, 22.0, 1.0, 2.0, 150),
          // Partial: atún natural has 0 g carbs/100g -> fibre/sugars confidently 0; sodium (brine/
          // salt varies by can) and sat-fat left null.
          new FoodItem("tuna", "Atún natural", 116, 25.0, 0.0, 1.0, 120, 0.0, 0.0, null, null),
          // Partial: merluza (white fish) has 0 g carbs/100g -> fibre/sugars confidently 0; sodium/
          // sat-fat vary by prep -> left null.
          new FoodItem("fish", "Merluza", 74, 16.0, 0.0, 1.0, 200, 0.0, 0.0, null, null),
          // Partial: salmón has 0 g carbs/100g -> fibre/sugars confidently 0; sat-fat is
          // meaningfully non-trivial for an oily fish but no specific figure is given -> left null.
          new FoodItem("salmon", "Salmón", 208, 20.0, 0.0, 13.0, 180, 0.0, 0.0, null, null),
          // Fully null: raw/dry rice's fibre/sodium/sat-fat depend on variety and cooking method,
          // not captured by the sheet's per-100g dry figures.
          new FoodItem("rice", "Arroz", 360, 7.0, 79.0, 1.0, 80),
          // Fully null: a branded whole-wheat pasta product.
          new FoodItem("whole-wheat-pasta", "Pasta integral", 350, 13.0, 70.0, 2.0, 80),
          // Fully null: raw vs cooked potato reference values differ and aren't disambiguated by
          // the sheet.
          new FoodItem("potato", "Patata", 77, 2.0, 17.0, 0.1, 300),
          // Fully null: boniato (sweet potato) has no confident reference in this dataset.
          new FoodItem("sweet-potato", "Boniato", 86, 1.6, 20.0, 0.1, 250),
          // Fully null: a branded whole-wheat bread product.
          new FoodItem("whole-wheat-bread", "Pan integral", 250, 9.0, 44.0, 4.0, 80),
          // Fully null: a generic "mixed vegetables" grouping has no single accurate composition.
          new FoodItem("vegetables", "Verdura variada", 35, 2.0, 6.0, 0.3, 300),
          // Fully null: a prepared/bagged salad mix varies by composition.
          new FoodItem("salad", "Ensalada preparada", 25, 1.5, 4.0, 0.2, 150),
          // Fully known: pure olive oil is a single ingredient with well-known, low-variance
          // composition (no carbs/protein -> fibre/sugars/sodium confidently 0; sat-fat is a
          // well-established figure for olive oil specifically).
          new FoodItem(
              "olive-oil",
              "Aceite oliva virgen extra",
              900,
              0.0,
              0.0,
              100.0,
              10,
              0.0,
              0.0,
              0.0,
              14.0),
          // Fully null: a mixed nut blend's exact ratio (and thus fibre/sat-fat split) varies.
          new FoodItem("almonds-walnuts", "Almendras/nueces", 600, 20.0, 10.0, 54.0, 25),
          // Fully null: a frozen mixed-berry blend varies by composition.
          new FoodItem("berries", "Frutos rojos congelados", 50, 1.0, 10.0, 0.5, 100),
          // Fully null: skimmed milk's exact sodium/fortification profile varies by brand.
          new FoodItem("skim-milk", "Leche desnatada", 35, 3.5, 5.0, 0.1, 250));

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

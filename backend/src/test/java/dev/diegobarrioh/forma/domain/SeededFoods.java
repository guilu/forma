package dev.diegobarrioh.forma.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 23 foods V25 seeds into {@code food_catalog}, as a {@link FoodLookup} for tests.
 *
 * <p>Production code reads those foods from the database. Domain tests cannot: {@link
 * NutritionCalculator} and {@link NutritionDayCatalog} are framework-free and take their foods as a
 * parameter, so the test has to supply them, and several assertions (daily protein bands, running
 * carbs vs rest carbs) only mean anything against the real seeded numbers.
 *
 * <p>This is test data that mirrors a migration, which can drift. {@code SeededFoodsMatchSeedTest}
 * asserts it row for row against a freshly migrated database, so drift fails a build instead of
 * quietly making these tests assert something the application never sees.
 *
 * <p>Values are copied verbatim from V25's INSERT, which itself came from the FOR-152 catalog. Null
 * key nutrients stay null — never fabricated (FOR-134).
 */
public final class SeededFoods {

  private static final List<FoodItem> FOODS =
      List.of(
          new FoodItem("oats", "Copos de avena", 370, 13.0, 60.0, 7.0, 60, 10.6, 0.0, 2.0, 1.2),
          new FoodItem(
              "whey-protein", "Whey proteína", 390, 78.0, 8.0, 6.0, 30, 0.0, null, null, null),
          new FoodItem("banana", "Plátano", 89, 1.1, 23.0, 0.3, 120, 2.6, 12.2, 1.0, 0.1),
          new FoodItem("eggs", "Huevos", 143, 13.0, 1.0, 10.0, 120, 0.0, null, 124.0, 3.3),
          new FoodItem(
              "egg-whites", "Claras líquidas", 48, 10.5, 0.7, 0.2, 150, 0.0, null, null, null),
          new FoodItem("fresh-cheese", "Queso fresco batido 0%", 46, 8.5, 3.5, 0.1, 250),
          new FoodItem("yogurt", "Yogur proteína", 59, 10.0, 4.0, 0.2, 200),
          new FoodItem("chicken", "Pechuga pollo", 110, 23.0, 0.0, 2.0, 200, 0.0, 0.0, null, null),
          new FoodItem("turkey", "Pavo lonchas/corte", 105, 22.0, 1.0, 2.0, 150),
          new FoodItem("tuna", "Atún natural", 116, 25.0, 0.0, 1.0, 120, 0.0, 0.0, null, null),
          new FoodItem("fish", "Merluza", 74, 16.0, 0.0, 1.0, 200, 0.0, 0.0, null, null),
          new FoodItem("salmon", "Salmón", 208, 20.0, 0.0, 13.0, 180, 0.0, 0.0, null, null),
          new FoodItem("rice", "Arroz", 360, 7.0, 79.0, 1.0, 80),
          new FoodItem("whole-wheat-pasta", "Pasta integral", 350, 13.0, 70.0, 2.0, 80),
          new FoodItem("potato", "Patata", 77, 2.0, 17.0, 0.1, 300),
          new FoodItem("sweet-potato", "Boniato", 86, 1.6, 20.0, 0.1, 250),
          new FoodItem("whole-wheat-bread", "Pan integral", 250, 9.0, 44.0, 4.0, 80),
          new FoodItem("vegetables", "Verdura variada", 35, 2.0, 6.0, 0.3, 300),
          new FoodItem("salad", "Ensalada preparada", 25, 1.5, 4.0, 0.2, 150),
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
          new FoodItem("almonds-walnuts", "Almendras/nueces", 600, 20.0, 10.0, 54.0, 25),
          new FoodItem("berries", "Frutos rojos congelados", 50, 1.0, 10.0, 0.5, 100),
          new FoodItem("skim-milk", "Leche desnatada", 35, 3.5, 5.0, 0.1, 250));

  private static final Map<String, FoodItem> BY_ID = new LinkedHashMap<>();

  static {
    FOODS.forEach(food -> BY_ID.put(food.id(), food));
  }

  /** The seeded foods as a lookup, ready to hand to any calculation that needs one. */
  public static final FoodLookup LOOKUP = id -> Optional.ofNullable(BY_ID.get(id));

  private SeededFoods() {}

  /** All seeded foods, in the migration's own row order. */
  public static List<FoodItem> all() {
    return FOODS;
  }

  /** A seeded food by id; fails the calling test when the id is not one of the 23. */
  public static FoodItem byId(String id) {
    FoodItem food = BY_ID.get(id);
    if (food == null) {
      throw new IllegalArgumentException("not a seeded food id: " + id);
    }
    return food;
  }
}

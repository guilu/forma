package dev.diegobarrioh.forma.domain;

import java.util.List;

/**
 * Computes nutrition totals for meals and days (FOR-32).
 *
 * <p>Pure, deterministic domain calculation (no forecasting), mirroring the FOR-21/FOR-28 summary
 * precedents. For each {@link MealItem} it resolves the {@link FoodItem} from the FOR-30 {@link
 * FoodCatalog} and adds {@code per100g * quantityG / 100} for each macro. Raw contributions are
 * summed and rounded once (via {@link NutritionTotals}) so sums do not accumulate rounding error.
 *
 * <p>An item referencing an unknown food id is rejected rather than skipped, so totals are never
 * silently understated (spec FOR-32 edge case).
 */
public final class NutritionCalculator {

  private NutritionCalculator() {}

  /** Totals for a single meal. */
  public static NutritionTotals mealTotals(MealTemplate meal) {
    return totals(meal.items());
  }

  /** Totals for a full day, summed over all its meals' items. */
  public static NutritionTotals dayTotals(List<MealTemplate> meals) {
    return totals(meals.stream().flatMap(meal -> meal.items().stream()).toList());
  }

  /**
   * Totals for a single {@link MealItem} (FOR-127): a food resolved from the FOR-30 {@link
   * FoodCatalog} plus a quantity. Reuses the same per-100g formula as {@link #mealTotals} and
   * {@link #dayTotals} — no duplicated math — so a consumption-log entry built from a catalog food
   * is computed identically to a plan-side meal item.
   */
  public static NutritionTotals itemTotals(MealItem item) {
    return totals(List.of(item));
  }

  /**
   * Key-nutrient totals for a single {@link MealItem} (FOR-134): fibre/sugars/sodium/saturated-fat
   * from the resolved {@link FoodItem}, scaled by the same {@code quantityG / 100.0} factor used by
   * {@link #itemTotals} for macros — no duplicated scaling logic, just applied to the four
   * additional nullable fields. A nutrient the food doesn't carry propagates as {@code null} (never
   * fabricated), independently per nutrient.
   */
  public static KeyNutrientTotals itemKeyNutrients(MealItem item) {
    FoodItem food =
        FoodCatalog.findById(item.foodItemId())
            .orElseThrow(
                () -> new IllegalArgumentException("unknown foodItemId: " + item.foodItemId()));
    double factor = item.quantityG() / 100.0;
    return new KeyNutrientTotals(
        scaleGrams(food.fiberPer100g(), factor),
        scaleGrams(food.sugarsPer100g(), factor),
        scaleMilligrams(food.sodiumMgPer100g(), factor),
        scaleGrams(food.saturatedFatPer100g(), factor));
  }

  private static Double scaleGrams(Double per100g, double factor) {
    return per100g == null ? null : round1(per100g * factor);
  }

  private static Integer scaleMilligrams(Double per100gMg, double factor) {
    return per100gMg == null ? null : (int) Math.round(per100gMg * factor);
  }

  private static NutritionTotals totals(List<MealItem> items) {
    double calories = 0;
    double protein = 0;
    double carbs = 0;
    double fat = 0;
    for (MealItem item : items) {
      FoodItem food =
          FoodCatalog.findById(item.foodItemId())
              .orElseThrow(
                  () -> new IllegalArgumentException("unknown foodItemId: " + item.foodItemId()));
      double factor = item.quantityG() / 100.0;
      calories += food.kcalPer100g() * factor;
      protein += food.proteinPer100g() * factor;
      carbs += food.carbsPer100g() * factor;
      fat += food.fatPer100g() * factor;
    }
    return new NutritionTotals(
        (int) Math.round(calories), round1(protein), round1(carbs), round1(fat));
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}

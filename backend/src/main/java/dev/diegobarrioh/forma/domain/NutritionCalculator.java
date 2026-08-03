package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Objects;

/**
 * Computes nutrition totals for meals and days (FOR-32).
 *
 * <p>Pure, deterministic domain calculation (no forecasting), mirroring the FOR-21/FOR-28 summary
 * precedents. For each {@link MealItem} it resolves the {@link FoodItem} through the
 * caller-supplied {@link FoodLookup} and adds {@code per100g * quantityG / 100} for each macro. Raw
 * contributions are summed and rounded once (via {@link NutritionTotals}) so sums do not accumulate
 * rounding error.
 *
 * <p>Foods arrive as a parameter rather than being read from a static catalog: the calculator
 * states what it needs and stays indifferent to whether those foods came from a database, a fixture
 * or a constant (see {@link FoodLookup}).
 *
 * <p>An item referencing an unknown food id is rejected rather than skipped, so totals are never
 * silently understated (spec FOR-32 edge case).
 */
public final class NutritionCalculator {

  private NutritionCalculator() {}

  /** Totals for a single meal. */
  public static NutritionTotals mealTotals(MealTemplate meal, FoodLookup foods) {
    return totals(meal.items(), foods);
  }

  /** Totals for a full day, summed over all its meals' items. */
  public static NutritionTotals dayTotals(List<MealTemplate> meals, FoodLookup foods) {
    return totals(meals.stream().flatMap(meal -> meal.items().stream()).toList(), foods);
  }

  /**
   * Totals for a single {@link MealItem} (FOR-127): a food resolved through {@code foods} plus a
   * quantity. Reuses the same per-100g formula as {@link #mealTotals} and {@link #dayTotals} — no
   * duplicated math — so a consumption-log entry built from a catalog food is computed identically
   * to a plan-side meal item.
   */
  public static NutritionTotals itemTotals(MealItem item, FoodLookup foods) {
    return totals(List.of(item), foods);
  }

  /**
   * Key-nutrient totals for a single {@link MealItem} (FOR-134): fibre/sugars/sodium/saturated-fat
   * from the resolved {@link FoodItem}, scaled by the same {@code quantityG / 100.0} factor used by
   * {@link #itemTotals} for macros — no duplicated scaling logic, just applied to the four
   * additional nullable fields. A nutrient the food doesn't carry propagates as {@code null} (never
   * fabricated), independently per nutrient.
   */
  public static KeyNutrientTotals itemKeyNutrients(MealItem item, FoodLookup foods) {
    Objects.requireNonNull(foods, "foods must not be null");
    FoodItem food = resolve(item, foods);
    double factor = item.quantityG() / 100.0;
    return new KeyNutrientTotals(
        scaleGrams(food.fiberPer100g(), factor),
        scaleGrams(food.sugarsPer100g(), factor),
        scaleMilligrams(food.sodiumMgPer100g(), factor),
        scaleGrams(food.saturatedFatPer100g(), factor));
  }

  private static FoodItem resolve(MealItem item, FoodLookup foods) {
    return foods
        .findById(item.foodItemId())
        .orElseThrow(
            () -> new IllegalArgumentException("unknown foodItemId: " + item.foodItemId()));
  }

  private static Double scaleGrams(Double per100g, double factor) {
    return per100g == null ? null : round1(per100g * factor);
  }

  private static Integer scaleMilligrams(Double per100gMg, double factor) {
    return per100gMg == null ? null : (int) Math.round(per100gMg * factor);
  }

  private static NutritionTotals totals(List<MealItem> items, FoodLookup foods) {
    // Checked up front, even for an empty day: a total computed with no lookup at all would pass
    // silently here and only blow up once someone added a first meal to that day.
    Objects.requireNonNull(foods, "foods must not be null");
    double calories = 0;
    double protein = 0;
    double carbs = 0;
    double fat = 0;
    for (MealItem item : items) {
      FoodItem food = resolve(item, foods);
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

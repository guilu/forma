package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MealTemplate;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing nutrition macro calculation (FOR-32).
 *
 * <p>Thin service over the pure {@link NutritionCalculator} domain calculation so later stories
 * (FOR-33 seed validation, and a future API/frontend) can compute meal/day totals and compare a day
 * to its targets. Mirrors the FOR-21/FOR-28 service pattern.
 *
 * <p>It also owns the foods those calculations run on: the calculator takes a lookup rather than
 * knowing where foods live, and this is the layer allowed to answer that (the persisted {@code
 * food_catalog}). Callers pass meals and get numbers, exactly as before.
 */
@Service
public class NutritionCalculationService {

  private final FoodCatalogService foods;

  public NutritionCalculationService(FoodCatalogService foods) {
    this.foods = foods;
  }

  /** Totals for a single meal. */
  public NutritionTotals mealTotals(MealTemplate meal) {
    return NutritionCalculator.mealTotals(meal, foods);
  }

  /** Totals for a full day (sum of its meals). */
  public NutritionTotals dayTotals(List<MealTemplate> meals) {
    return NutritionCalculator.dayTotals(meals, foods);
  }

  /**
   * The display name of a catalog food, falling back to the id itself when nothing has that id.
   *
   * <p>For read models that show a meal's items: they hold food ids, and a name has to come from
   * the same catalog the macros came from. The fallback keeps a stale id visible rather than
   * blanking the item — a meal that lists "oats" tells whoever is looking far more than one that
   * lists nothing.
   */
  public String foodName(String foodItemId) {
    return foods.findById(foodItemId).map(FoodItem::name).orElse(foodItemId);
  }

  /** Whether a day's totals reach the given day template's targets, per macro. */
  public TargetComparison compareToTargets(List<MealTemplate> meals, NutritionDayTemplate target) {
    return TargetComparison.of(dayTotals(meals), target);
  }
}

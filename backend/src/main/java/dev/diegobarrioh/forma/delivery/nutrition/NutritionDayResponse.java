package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDay;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/days/{type}} (FOR-34, enriched by FOR-105).
 *
 * <p>Delivery read model, distinct from the application/domain {@link NutritionDay} (ADR-005).
 * Meals are ordered by preferred time; the post-workout meal is flagged {@code optional} so the UI
 * can present the late-run recovery item as skippable. Food ids are resolved to names via the
 * FOR-30 catalog.
 *
 * <p>FOR-105 adds per-meal and per-day macro {@code totals} and a {@code targetComparison}, both
 * delegated to the FOR-32 {@link NutritionCalculationService} — no macro math happens here. These
 * are PLAN macros vs target, not consumed/logged intake (FOR-102).
 */
public record NutritionDayResponse(
    String type,
    Targets targets,
    Totals totals,
    TargetComparison targetComparison,
    List<Meal> meals) {

  public record Targets(int calories, int proteinG, int carbsG, int fatG) {}

  /** A meal or day's computed macro totals (FOR-32 {@link NutritionTotals}, carried as-is). */
  public record Totals(int calories, double proteinG, double carbsG, double fatG) {

    static Totals from(NutritionTotals totals) {
      return new Totals(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }
  }

  /** Whether the day's totals reach its targets, per macro (FOR-32 {@code TargetComparison}). */
  public record TargetComparison(
      boolean caloriesReached, boolean proteinReached, boolean carbsReached, boolean fatReached) {

    static TargetComparison from(dev.diegobarrioh.forma.domain.TargetComparison comparison) {
      return new TargetComparison(
          comparison.caloriesReached(),
          comparison.proteinReached(),
          comparison.carbsReached(),
          comparison.fatReached());
    }
  }

  public record Meal(
      String mealType,
      String name,
      String preferredTime,
      boolean optional,
      Totals totals,
      List<Item> items) {}

  public record Item(String food, int quantityG) {}

  /**
   * Maps a seeded nutrition day to its API read model, delegating macro totals and the target
   * comparison to the FOR-32 {@link NutritionCalculationService} (ADR-001: no math here).
   */
  public static NutritionDayResponse from(NutritionDay day, NutritionCalculationService calc) {
    Targets targets =
        new Targets(
            day.template().targetCalories(),
            day.template().targetProteinG(),
            day.template().targetCarbsG(),
            day.template().targetFatG());
    List<Meal> meals =
        day.meals().stream()
            .map(
                meal ->
                    new Meal(
                        meal.mealType().name(),
                        meal.name(),
                        meal.preferredTime().toString(),
                        meal.mealType() == MealType.POST_WORKOUT,
                        Totals.from(calc.mealTotals(meal)),
                        meal.items().stream()
                            .map(
                                item ->
                                    new Item(
                                        FoodCatalog.findById(item.foodItemId())
                                            .map(food -> food.name())
                                            .orElse(item.foodItemId()),
                                        item.quantityG()))
                            .toList()))
            .toList();
    Totals dayTotals = Totals.from(calc.dayTotals(day.meals()));
    TargetComparison targetComparison =
        TargetComparison.from(calc.compareToTargets(day.meals(), day.template()));
    return new NutritionDayResponse(
        day.template().type().name(), targets, dayTotals, targetComparison, meals);
  }
}

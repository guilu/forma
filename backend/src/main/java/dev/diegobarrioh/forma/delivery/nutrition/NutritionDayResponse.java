package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDay;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/days/{type}} (FOR-34).
 *
 * <p>Delivery read model, distinct from the application/domain {@link NutritionDay} (ADR-005).
 * Meals are ordered by preferred time; the post-workout meal is flagged {@code optional} so the UI
 * can present the late-run recovery item as skippable. Food ids are resolved to names via the
 * FOR-30 catalog.
 */
public record NutritionDayResponse(String type, Targets targets, List<Meal> meals) {

  public record Targets(int calories, int proteinG, int carbsG, int fatG) {}

  public record Meal(
      String mealType, String name, String preferredTime, boolean optional, List<Item> items) {}

  public record Item(String food, int quantityG) {}

  /** Maps a seeded nutrition day to its API read model. */
  public static NutritionDayResponse from(NutritionDay day) {
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
    return new NutritionDayResponse(day.template().type().name(), targets, meals);
  }
}

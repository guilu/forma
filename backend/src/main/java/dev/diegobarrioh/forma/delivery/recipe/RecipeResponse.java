package dev.diegobarrioh.forma.delivery.recipe;

import dev.diegobarrioh.forma.application.ResolvedRecipe;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for a recipe (V52).
 *
 * <p>Delivery read model, distinct from the application types (ADR-005). The dish and what it works
 * out to travel together: a client asking for a recipe wants its macros, and making it fetch every
 * ingredient's food and sum them is how a second, divergent copy of that sum gets written.
 *
 * <p>{@code total} and {@code perServing} are computed at the moment of the request. They are not
 * stored, so two calls a month apart may legitimately disagree if somebody corrected a food in
 * between — that is the design working.
 */
public record RecipeResponse(
    String id,
    String name,
    int servings,
    String notes,
    List<IngredientResponse> ingredients,
    Totals total,
    Totals perServing,
    /**
     * Ingredients whose food is no longer in the catalog. Should always be empty — a foreign key
     * protects them — and travels so a dish with one bad line still renders instead of vanishing.
     */
    List<String> unknownFoodIds) {

  public record IngredientResponse(String foodId, BigDecimal grams) {}

  public record Totals(int calories, double proteinG, double carbsG, double fatG) {}

  public static RecipeResponse from(ResolvedRecipe resolved) {
    var recipe = resolved.recipe();
    return new RecipeResponse(
        recipe.id(),
        recipe.name(),
        recipe.servings(),
        recipe.notes(),
        recipe.ingredients().stream()
            .map(line -> new IngredientResponse(line.foodId(), line.grams()))
            .toList(),
        totals(resolved.total()),
        totals(resolved.perServing()),
        resolved.unknownFoodIds());
  }

  private static Totals totals(dev.diegobarrioh.forma.domain.NutritionTotals from) {
    return new Totals(from.calories(), from.proteinG(), from.carbsG(), from.fatG());
  }
}

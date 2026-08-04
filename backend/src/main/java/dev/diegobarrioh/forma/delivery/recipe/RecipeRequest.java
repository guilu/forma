package dev.diegobarrioh.forma.delivery.recipe;

import dev.diegobarrioh.forma.application.Recipe;
import dev.diegobarrioh.forma.application.RecipeIngredient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Body accepted when writing a recipe (V52, admin only).
 *
 * <p>Carries no nutrition. The totals are the sum over the ingredients of what the catalog holds,
 * so accepting them would offer numbers the server has to ignore — and that somebody would
 * eventually notice disagreeing with the ones it computes.
 *
 * @param servings how many portions the whole thing makes; at least one
 * @param ingredients at least one, because a dish with nothing in it totals zero and means nothing
 */
public record RecipeRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9-]{1,64}") String id,
    @NotBlank @Size(max = 200) String name,
    @Positive int servings,
    String notes,
    @NotEmpty @Valid List<IngredientRequest> ingredients) {

  /** One food and how much of it goes in. */
  public record IngredientRequest(
      @NotBlank @Size(max = 64) String foodId, @DecimalMin("0.1") BigDecimal grams) {}

  /** Maps the request onto the application's own type. */
  public Recipe toRecipe() {
    List<RecipeIngredient> lines = new java.util.ArrayList<>();
    for (int index = 0; index < ingredients.size(); index++) {
      IngredientRequest line = ingredients.get(index);
      // The order they were sent in is the order somebody would read them out, so it is kept
      // rather than asked for as a field nobody would fill correctly.
      lines.add(new RecipeIngredient(line.foodId(), line.grams(), index));
    }
    return new Recipe(id, name, servings, notes, true, List.copyOf(lines), null, null);
  }
}

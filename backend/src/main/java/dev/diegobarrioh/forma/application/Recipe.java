package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.List;

/**
 * A named list of foods with amounts (V52).
 *
 * <p>Read model for a {@code recipe} row and its ingredients. It holds no nutrition of its own: the
 * totals are the sum over the ingredients of what {@code food_catalog} says, so storing them would
 * freeze an answer that has to move when somebody corrects a food. Same reasoning that kept {@code
 * ratio} out of the equivalences (V47).
 *
 * <p>Deliberately close to {@link dev.diegobarrioh.forma.domain.MealTemplate}, which is the same
 * shape plus a time and a day type and is not persisted. A day's meal is a recipe placed somewhere,
 * and the honest model has one referencing the other; that rewiring waits for the meal-plan
 * document rather than being guessed at now. See V52's comment.
 *
 * @param id a slug, stable and readable in a URL
 * @param name what the dish is called; unique, because two dishes reading the same would make a
 *     list say one thing twice
 * @param servings how many portions the whole thing makes. A stew for four read as a meal for one
 *     makes every per-serving figure wrong fourfold
 * @param ingredients what goes in, in the order somebody would read them
 */
public record Recipe(
    String id,
    String name,
    int servings,
    String notes,
    boolean enabled,
    List<RecipeIngredient> ingredients,
    Instant createdAt,
    Instant updatedAt) {

  public Recipe {
    if (servings <= 0) {
      throw new IllegalArgumentException("servings must be strictly positive, was: " + servings);
    }
    ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
  }
}

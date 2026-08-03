package dev.diegobarrioh.forma.application;

import java.math.BigDecimal;

/**
 * One food in a recipe, and how much of it (V52).
 *
 * <p>Grams, as the catalog records them. A recipe listing 80 g of rice is listing it dry, because
 * that is the state {@code food_catalog} holds it in (V51) — the dish it produces is cooked, and
 * this table records what goes in rather than what comes out.
 *
 * @param foodId the food; one line per food, so an amount is never split across two rows
 * @param grams how much goes in; strictly positive
 * @param sortOrder where it sits in the list, in the order somebody would read them out
 */
public record RecipeIngredient(String foodId, BigDecimal grams, int sortOrder) {

  public RecipeIngredient {
    if (foodId == null || foodId.isBlank()) {
      throw new IllegalArgumentException("foodId must not be blank");
    }
    if (grams == null || grams.signum() <= 0) {
      throw new IllegalArgumentException("grams must be strictly positive, was: " + grams);
    }
  }
}

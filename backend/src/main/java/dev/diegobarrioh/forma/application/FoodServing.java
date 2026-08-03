package dev.diegobarrioh.forma.application;

import java.math.BigDecimal;

/**
 * One portion of one food (V49).
 *
 * <p>Read model for a {@code food_serving} row. A food used to carry exactly one of these as a
 * column, which forced every food to have a single sensible portion — wrong for most of them, since
 * a banana is small, medium or large and oil is a teaspoon or a splash.
 *
 * @param id ours; the backfilled default of each food took the food's own id, and anything written
 *     later gets a minted one. Both are opaque strings
 * @param foodId the food this is a portion of
 * @param name what to call it — "Mediano", "Cucharada" — or {@code null} for the plain one a food
 *     starts with, which is less "the unnamed portion" than "the portion, before anybody bothered
 *     to distinguish sizes"
 * @param grams how much it weighs
 * @param isDefault whether this is the one meant by "one serving". Exactly one per food at most,
 *     enforced by a nullable sentinel because H2 cannot express the partial index that would be the
 *     obvious way (ADR-011)
 * @param sortOrder where it sits among the food's other portions
 */
public record FoodServing(
    String id, String foodId, String name, BigDecimal grams, boolean isDefault, int sortOrder) {

  public FoodServing {
    if (grams == null || grams.signum() <= 0) {
      throw new IllegalArgumentException("grams must be strictly positive, was: " + grams);
    }
  }

  /** The portion a food starts with: unnamed, default, first. */
  public static FoodServing plainDefault(String foodId, BigDecimal grams) {
    return new FoodServing(foodId, foodId, null, grams, true, 0);
  }
}

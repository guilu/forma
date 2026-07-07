package dev.diegobarrioh.forma.domain;

/**
 * One food entry within a {@link MealTemplate} (FOR-31), per docs/domain-model.md's "MealItem".
 *
 * <p>Framework-free (ADR-001). References a {@link FoodItem} by its stable {@link #foodItemId}
 * (FOR-30) rather than embedding nutrition values; macro calculation (FOR-32) resolves the food and
 * uses {@link #quantityG}. Referential integrity (the id existing in the FOR-30 catalog) is
 * enforced where meals are built/seeded (FOR-33), not by this type.
 *
 * @param foodItemId stable id of a FOR-30 catalog food; required, non-blank
 * @param quantityG amount of the food in grams; must be strictly positive
 */
public record MealItem(String foodItemId, int quantityG) {

  public MealItem {
    if (foodItemId == null || foodItemId.isBlank()) {
      throw new IllegalArgumentException("foodItemId must not be blank");
    }
    if (quantityG <= 0) {
      throw new IllegalArgumentException("quantityG must be strictly positive, was: " + quantityG);
    }
  }
}

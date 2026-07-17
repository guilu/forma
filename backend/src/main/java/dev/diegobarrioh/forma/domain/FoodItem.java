package dev.diegobarrioh.forma.domain;

/**
 * A food item's nutrition values (FOR-30) — reference data, not a store product.
 *
 * <p>Framework-free domain type (ADR-001), per docs/domain-model.md's "FoodItem". Meal items
 * (FOR-31) reference a food by its stable {@link #id}; macro calculation (FOR-32) uses the per-100
 * g values × the meal item grams. Product/brand/price fields belong to the Shopping context (FOR-5,
 * {@code MercadonaProduct}) and are deliberately absent here.
 *
 * <p>Values are validated at construction (FOR-15/FOR-24 precedent).
 *
 * <p><b>Key nutrients (FOR-134).</b> {@link #fiberPer100g}, {@link #sugarsPer100g}, {@link
 * #sodiumMgPer100g} and {@link #saturatedFatPer100g} are deliberately nullable: this MVP reference
 * catalog does not have verified data for every food/nutrient combination, and a missing value must
 * stay {@code null} rather than being fabricated (see {@link FoodCatalog}). {@link
 * #sodiumMgPer100g} is in milligrams (the conventional unit for sodium); the other three are in
 * grams, matching the existing macro fields.
 *
 * @param id stable identifier used by meal items to reference this food; required, non-blank
 * @param name human-readable food name; required, non-blank
 * @param kcalPer100g energy per 100 g in kilocalories; must be strictly positive
 * @param proteinPer100g protein grams per 100 g; must be >= 0
 * @param carbsPer100g carbohydrate grams per 100 g; must be >= 0
 * @param fatPer100g fat grams per 100 g; must be >= 0
 * @param defaultServingG a sensible default serving in grams; must be strictly positive
 * @param fiberPer100g fibre grams per 100 g, or {@code null} if unknown; must be >= 0 when present
 * @param sugarsPer100g sugars grams per 100 g, or {@code null} if unknown; must be >= 0 when
 *     present
 * @param sodiumMgPer100g sodium milligrams per 100 g, or {@code null} if unknown; must be >= 0 when
 *     present
 * @param saturatedFatPer100g saturated fat grams per 100 g, or {@code null} if unknown; must be >=
 *     0 when present
 */
public record FoodItem(
    String id,
    String name,
    int kcalPer100g,
    double proteinPer100g,
    double carbsPer100g,
    double fatPer100g,
    int defaultServingG,
    Double fiberPer100g,
    Double sugarsPer100g,
    Double sodiumMgPer100g,
    Double saturatedFatPer100g) {

  public FoodItem {
    requireText(id, "id");
    requireText(name, "name");
    if (kcalPer100g <= 0) {
      throw new IllegalArgumentException(
          "kcalPer100g must be strictly positive, was: " + kcalPer100g);
    }
    requireNonNegative(proteinPer100g, "proteinPer100g");
    requireNonNegative(carbsPer100g, "carbsPer100g");
    requireNonNegative(fatPer100g, "fatPer100g");
    if (defaultServingG <= 0) {
      throw new IllegalArgumentException(
          "defaultServingG must be strictly positive, was: " + defaultServingG);
    }
    requireNonNegativeIfPresent(fiberPer100g, "fiberPer100g");
    requireNonNegativeIfPresent(sugarsPer100g, "sugarsPer100g");
    requireNonNegativeIfPresent(sodiumMgPer100g, "sodiumMgPer100g");
    requireNonNegativeIfPresent(saturatedFatPer100g, "saturatedFatPer100g");
  }

  /**
   * Convenience constructor for a food with no known key nutrients (pre-FOR-134 shape) — all four
   * key-nutrient fields default to {@code null} (unknown), never fabricated.
   */
  public FoodItem(
      String id,
      String name,
      int kcalPer100g,
      double proteinPer100g,
      double carbsPer100g,
      double fatPer100g,
      int defaultServingG) {
    this(
        id,
        name,
        kcalPer100g,
        proteinPer100g,
        carbsPer100g,
        fatPer100g,
        defaultServingG,
        null,
        null,
        null,
        null);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }

  private static void requireNonNegative(double value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must be >= 0, was: " + value);
    }
  }

  private static void requireNonNegativeIfPresent(Double value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must be >= 0, was: " + value);
    }
  }
}

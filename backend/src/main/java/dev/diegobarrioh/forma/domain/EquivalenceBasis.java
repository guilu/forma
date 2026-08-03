package dev.diegobarrioh.forma.domain;

/**
 * Which nutrient an equivalence keeps equal (V47).
 *
 * <p>Stated explicitly rather than inferred. A "dominant macro" criterion was considered and
 * rejected: it reads well and means nothing precise, since a curator swapping rice for potato is
 * matching carbohydrate on purpose, not because the arithmetic happened to make carbohydrate the
 * biggest number. What is being matched is a decision, and decisions get written down.
 *
 * <p>A closed set for the same reason as {@link PrimaryMacro}: calories and the three
 * macronutrients are all there is to hold equal. Hence an enum and a CHECK, not a table.
 */
public enum EquivalenceBasis {
  CALORIES,
  PROTEIN,
  CARBS,
  FAT;

  /** How much of this nutrient 100 g of the food carries. */
  public double per100gOf(FoodItem food) {
    return switch (this) {
      case CALORIES -> food.kcalPer100g();
      case PROTEIN -> food.proteinPer100g();
      case CARBS -> food.carbsPer100g();
      case FAT -> food.fatPer100g();
    };
  }
}

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealType;
import java.time.LocalDate;

/**
 * Input to {@link MealLogService#log}: either a FOR-30 catalog food reference ({@code foodItemId} +
 * {@code portions}) or a free/ad-hoc entry ({@code name} + macros) — exactly one of the two shapes
 * must be populated, validated by {@link MealLogService} (spec FOR-127 api.md). Since FOR-134, a
 * free entry may also optionally supply key nutrients; a catalog entry's key nutrients always come
 * from the resolved {@link dev.diegobarrioh.forma.domain.FoodItem} instead, so these fields are
 * ignored for a catalog entry.
 *
 * @param date the day the meal was consumed
 * @param mealType the meal type
 * @param foodItemId FOR-30 catalog food id, or {@code null} for a free entry
 * @param portions number of the food's DEFAULT servings, or {@code null}
 * @param grams the amount in grams, or {@code null}
 * @param servingId a named portion of that food (V49) that {@code portions} counts, or {@code null}
 *     to count the default one
 * @param name free entry's name, or {@code null} for a catalog entry (the catalog food's name is
 *     used instead)
 * @param kcal free entry's calories, or {@code null} for a catalog entry
 * @param proteinG free entry's protein grams, or {@code null} for a catalog entry
 * @param carbsG free entry's carbohydrate grams, or {@code null} for a catalog entry
 * @param fatG free entry's fat grams, or {@code null} for a catalog entry
 * @param fiberG free entry's optional fibre grams (FOR-134), or {@code null} if not provided
 * @param sugarsG free entry's optional sugars grams (FOR-134), or {@code null} if not provided
 * @param sodiumMg free entry's optional sodium milligrams (FOR-134), or {@code null} if not
 *     provided
 * @param saturatedFatG free entry's optional saturated fat grams (FOR-134), or {@code null} if not
 *     provided
 * @param plannedMealId which planned meal this answers (V55), or {@code null} — the ordinary case,
 *     meaning nothing in any plan asked for it. Independent of the two shapes above: a planned meal
 *     can be logged from the catalog or as a free entry, and eating something else instead is still
 *     an answer to it
 */
public record LogMealCommand(
    LocalDate date,
    MealType mealType,
    String foodItemId,
    Double portions,
    Double grams,
    String servingId,
    String name,
    Integer kcal,
    Double proteinG,
    Double carbsG,
    Double fatG,
    Double fiberG,
    Double sugarsG,
    Integer sodiumMg,
    Double saturatedFatG,
    java.util.UUID plannedMealId) {

  /** Builds a catalog-entry command counting the food's DEFAULT portion. */
  public static LogMealCommand catalog(
      LocalDate date, MealType mealType, String foodItemId, double portions) {
    return catalogEntry(date, mealType, foodItemId, portions, null, null);
  }

  /** Builds a catalog-entry command measured in grams, which every food can be. */
  public static LogMealCommand catalogGrams(
      LocalDate date, MealType mealType, String foodItemId, double grams) {
    return catalogEntry(date, mealType, foodItemId, null, grams, null);
  }

  /** Builds a catalog-entry command counting a named portion of that food (V49). */
  public static LogMealCommand catalogServings(
      LocalDate date, MealType mealType, String foodItemId, String servingId, double count) {
    return catalogEntry(date, mealType, foodItemId, count, null, servingId);
  }

  private static LogMealCommand catalogEntry(
      LocalDate date,
      MealType mealType,
      String foodItemId,
      Double portions,
      Double grams,
      String servingId) {
    return new LogMealCommand(
        date,
        mealType,
        foodItemId,
        portions,
        grams,
        servingId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** Builds a free/ad-hoc entry command with no key nutrients. */
  public static LogMealCommand free(
      LocalDate date,
      MealType mealType,
      String name,
      int kcal,
      double proteinG,
      double carbsG,
      double fatG) {
    return new LogMealCommand(
        date, mealType, null, null, null, null, name, kcal, proteinG, carbsG, fatG, null, null,
        null, null, null);
  }
}

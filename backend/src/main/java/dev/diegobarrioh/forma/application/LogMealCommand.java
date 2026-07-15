package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealType;
import java.time.LocalDate;

/**
 * Input to {@link MealLogService#log}: either a FOR-30 catalog food reference ({@code foodItemId} +
 * {@code portions}) or a free/ad-hoc entry ({@code name} + macros) — exactly one of the two shapes
 * must be populated, validated by {@link MealLogService} (spec FOR-127 api.md).
 *
 * @param date the day the meal was consumed
 * @param mealType the meal type
 * @param foodItemId FOR-30 catalog food id, or {@code null} for a free entry
 * @param portions number of the food's default servings, or {@code null} for a free entry
 * @param name free entry's name, or {@code null} for a catalog entry (the catalog food's name is
 *     used instead)
 * @param kcal free entry's calories, or {@code null} for a catalog entry
 * @param proteinG free entry's protein grams, or {@code null} for a catalog entry
 * @param carbsG free entry's carbohydrate grams, or {@code null} for a catalog entry
 * @param fatG free entry's fat grams, or {@code null} for a catalog entry
 */
public record LogMealCommand(
    LocalDate date,
    MealType mealType,
    String foodItemId,
    Double portions,
    String name,
    Integer kcal,
    Double proteinG,
    Double carbsG,
    Double fatG) {

  /** Builds a catalog-entry command. */
  public static LogMealCommand catalog(
      LocalDate date, MealType mealType, String foodItemId, double portions) {
    return new LogMealCommand(date, mealType, foodItemId, portions, null, null, null, null, null);
  }

  /** Builds a free/ad-hoc entry command. */
  public static LogMealCommand free(
      LocalDate date,
      MealType mealType,
      String name,
      int kcal,
      double proteinG,
      double carbsG,
      double fatG) {
    return new LogMealCommand(date, mealType, null, null, name, kcal, proteinG, carbsG, fatG);
  }
}

package dev.diegobarrioh.forma.domain;

import java.util.Objects;

/**
 * Macro targets for a nutrition day type (FOR-29).
 *
 * <p>The Nutrition context's day-target model (docs/domain-model.md, "NutritionDayTemplate"). It is
 * framework-free — no Spring, JPA/JDBC or HTTP types (ADR-001) — following the FOR-22/FOR-24
 * precedents. It carries <em>targets</em> only; the day's meals are FOR-31 {@code MealTemplate}s
 * that reference this day type, and foods are the FOR-30 catalog.
 *
 * <p>Targets are directional (not medical prescriptions, spec FOR-33) and editable later. Calorie/
 * macro coherence (4/4/9 kcal per gram) is intentionally not enforced here.
 *
 * <p>Values are validated at construction (positive targets) for internal consistency, following
 * the FOR-15/FOR-22 precedent.
 *
 * @param type the day type; required
 * @param targetCalories daily calorie target (kcal); must be strictly positive
 * @param targetProteinG daily protein target in grams; must be strictly positive
 * @param targetCarbsG daily carbohydrate target in grams; must be strictly positive
 * @param targetFatG daily fat target in grams; must be strictly positive
 * @param notes optional free-text note
 */
public record NutritionDayTemplate(
    NutritionDayType type,
    int targetCalories,
    int targetProteinG,
    int targetCarbsG,
    int targetFatG,
    String notes) {

  public NutritionDayTemplate {
    Objects.requireNonNull(type, "type must not be null");
    requirePositive(targetCalories, "targetCalories");
    requirePositive(targetProteinG, "targetProteinG");
    requirePositive(targetCarbsG, "targetCarbsG");
    requirePositive(targetFatG, "targetFatG");
  }

  private static void requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be strictly positive, was: " + value);
    }
  }
}

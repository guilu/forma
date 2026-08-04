package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalTime;
import java.util.List;

/**
 * A planned meal with its lines worked out and its own total (V53/V54).
 *
 * <p>{@link #optional} is read from the plan, not inferred. It used to be {@code mealType ==
 * POST_WORKOUT} written into the delivery layer — a decision about one specific seeded plan,
 * applied to every plan there would ever be.
 *
 * @param id the planned meal's own id (V55), so an entry logged against it can be matched back
 * @param mealType which moment of the day this is
 * @param name what to call it
 * @param scheduledTime when it is meant to happen; null when nobody fixed one
 * @param optional whether the meal can be skipped
 * @param instructions the rule, for a meal that is a rule rather than a list
 * @param targets what this meal was asked to hit; possibly unset
 * @param totals what its lines come to
 * @param items its lines
 */
public record ResolvedMeal(
    java.util.UUID id,
    MealType mealType,
    String name,
    LocalTime scheduledTime,
    boolean optional,
    String instructions,
    MacroTargets targets,
    NutritionTotals totals,
    List<ResolvedItem> items) {

  public ResolvedMeal {
    items = List.copyOf(items);
  }
}

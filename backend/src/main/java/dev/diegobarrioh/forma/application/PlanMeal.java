package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealType;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One meal of a planned day (V53).
 *
 * <p>The persisted successor to {@code domain.MealTemplate}, which held the same shape as constants
 * in {@code NutritionDayCatalog}. Three things it can say that its predecessor could not: it
 * carries its own {@link #targets} (how the day's total is meant to be distributed across its
 * meals), it can be {@link #optional} as a stored fact rather than as {@code mealType ==
 * POST_WORKOUT} hardcoded into the delivery layer, and it can be a rule instead of a list — {@link
 * #instructions} holds "una proteína, un carbohidrato y una verdura" for a meal that is
 * deliberately not closed.
 *
 * <p>No {@code sortOrder}, for the same reason {@link PlanItem} has none: the day's meals are a
 * list, and its order is the list's. The column is filled from the position by the repository.
 *
 * <p>Its macros are not here and never will be: they are the sum over {@link #items} of what the
 * catalog holds, computed on read (ADR-011).
 *
 * @param id the row's id; null before it has been written
 * @param mealType which moment of the day this is; required
 * @param name what to call it; required, non-blank
 * @param scheduledTime the time it is meant to happen; null when nobody fixed one
 * @param targets what this meal was asked to hit; never null, possibly unset
 * @param instructions free text for a meal that is a rule rather than a list
 * @param optional whether the meal can be skipped
 * @param items the meal's lines; may be empty when {@link #instructions} carries the whole meal
 */
public record PlanMeal(
    UUID id,
    MealType mealType,
    String name,
    LocalTime scheduledTime,
    MacroTargets targets,
    String instructions,
    boolean optional,
    List<PlanItem> items) {

  public PlanMeal {
    Objects.requireNonNull(mealType, "mealType must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    targets = targets == null ? MacroTargets.none() : targets;
    items = items == null ? List.of() : List.copyOf(items);
    // A meal with neither lines nor instructions says nothing at all — not "eat nothing", which is
    // what an empty list would be read as, but "somebody opened this and never filled it in".
    if (items.isEmpty() && (instructions == null || instructions.isBlank())) {
      throw new IllegalArgumentException(
          "a meal must have items or instructions; an empty one says nothing");
    }
  }
}

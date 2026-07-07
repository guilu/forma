package dev.diegobarrioh.forma.domain;

import java.util.List;

/**
 * A seeded nutrition day (FOR-33): a {@link NutritionDayTemplate} (targets) together with the
 * {@link MealTemplate}s that make up that day.
 *
 * <p>Framework-free bundle. Its target macros are the computed totals of its meals (FOR-32), so the
 * default plan is self-consistent; the user edits it later.
 *
 * @param template the day's macro targets and type
 * @param meals the day's meals, in preferred-time order
 */
public record NutritionDay(NutritionDayTemplate template, List<MealTemplate> meals) {

  public NutritionDay {
    meals = List.copyOf(meals);
  }
}

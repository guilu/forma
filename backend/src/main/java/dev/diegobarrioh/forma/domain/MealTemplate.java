package dev.diegobarrioh.forma.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * A reusable meal within a nutrition day (FOR-31), per docs/domain-model.md's "MealTemplate".
 *
 * <p>Framework-free (ADR-001). A meal belongs to a nutrition day type ({@link #dayType}, FOR-29)
 * and contains one or more {@link MealItem}s that reference FOR-30 catalog foods by id. Macros are
 * computed by FOR-32 from the items; this type holds no nutrition totals.
 *
 * <p>The day is referenced by {@link NutritionDayType} (templates are identified by type in the
 * MVP) and {@link #preferredTime} is a {@link LocalTime} for structure (spec FOR-31 Open
 * Questions). Templates are reusable and editable later.
 *
 * @param dayType the nutrition day this meal belongs to; required
 * @param mealType the meal type; required
 * @param name human-readable meal name; required, non-blank
 * @param preferredTime the preferred time of day for the meal; required
 * @param items the meal's food entries; required, non-empty
 * @param notes optional free-text note
 */
public record MealTemplate(
    NutritionDayType dayType,
    MealType mealType,
    String name,
    LocalTime preferredTime,
    List<MealItem> items,
    String notes) {

  public MealTemplate {
    Objects.requireNonNull(dayType, "dayType must not be null");
    Objects.requireNonNull(mealType, "mealType must not be null");
    Objects.requireNonNull(preferredTime, "preferredTime must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(items, "items must not be null");
    if (items.isEmpty()) {
      throw new IllegalArgumentException("a meal template must have at least one item");
    }
    items = List.copyOf(items);
  }
}

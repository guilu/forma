package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single logged (consumed) meal entry (FOR-127, first implementable slice of FOR-102): either a
 * FOR-30 catalog {@link FoodItem} scaled by a portion count, or a free/ad-hoc entry with macros
 * supplied directly by the caller. Macros only (kcal/protein/carbs/fat) — hydration and key
 * nutrients (fibra/azúcares/sodio/grasas saturadas) are later FOR-102 slices, out of scope here.
 *
 * <p>Framework-free (ADR-001). {@link #totals} is a snapshot of what was actually consumed,
 * computed once at logging time — so a later change to the FOR-30 catalog never rewrites history,
 * and a per-day aggregate ({@link MealLog}) can sum entries without recomputing catalog math on
 * every read. This is strictly additive: it never reads or writes a {@link NutritionDayTemplate} or
 * {@link MealTemplate} (spec FOR-127: "logging is additive — must NOT mutate any plan template").
 *
 * @param date the day the meal was consumed
 * @param mealType the meal type; required
 * @param name human-readable name — the catalog food's name, or the free entry's provided name
 * @param foodItemId the FOR-30 catalog food id, or {@code null} for a free/ad-hoc entry
 * @param totals the entry's macro totals, computed once at logging time
 */
public record MealLogEntry(
    LocalDate date, MealType mealType, String name, String foodItemId, NutritionTotals totals) {

  public MealLogEntry {
    Objects.requireNonNull(date, "date must not be null");
    Objects.requireNonNull(mealType, "mealType must not be null");
    Objects.requireNonNull(totals, "totals must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  /**
   * Builds an entry from a resolved FOR-30 {@code food} and a portion count (number of the food's
   * {@link FoodItem#defaultServingG} servings). Macro math is delegated to {@link
   * NutritionCalculator#itemTotals} (FOR-32) — no duplicated formula here. The caller (application
   * layer) is responsible for resolving {@code foodItemId} to a {@link FoodItem} first, so an
   * unknown id surfaces as a caller-input validation error rather than a domain exception.
   *
   * @param portions must be strictly positive; quantity in grams is {@code portions *
   *     food.defaultServingG()}, rounded to the nearest gram
   */
  public static MealLogEntry fromCatalog(
      LocalDate date, MealType mealType, FoodItem food, double portions) {
    Objects.requireNonNull(food, "food must not be null");
    if (portions <= 0) {
      throw new IllegalArgumentException("portions must be strictly positive, was: " + portions);
    }
    int quantityG = (int) Math.round(portions * food.defaultServingG());
    if (quantityG <= 0) {
      quantityG = 1;
    }
    NutritionTotals totals = NutritionCalculator.itemTotals(new MealItem(food.id(), quantityG));
    return new MealLogEntry(date, mealType, food.name(), food.id(), totals);
  }

  /** Builds a free/ad-hoc entry: macros are supplied directly, no catalog food is referenced. */
  public static MealLogEntry freeEntry(
      LocalDate date, MealType mealType, String name, NutritionTotals totals) {
    return new MealLogEntry(date, mealType, name, null, totals);
  }
}

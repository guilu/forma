package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single logged (consumed) meal entry (FOR-127, first implementable slice of FOR-102): either a
 * FOR-30 catalog {@link FoodItem} scaled by a portion count, or a free/ad-hoc entry with macros
 * supplied directly by the caller. Macros (kcal/protein/carbs/fat) plus, since FOR-134, key
 * nutrients (fibra/azúcares/sodio/grasas saturadas) — hydration is a separate FOR-102 slice, out of
 * scope here.
 *
 * <p>Framework-free (ADR-001). {@link #totals} and {@link #keyNutrients} are snapshots of what was
 * actually consumed, computed once at logging time — so a later change to the FOR-30 catalog never
 * rewrites history, and a per-day aggregate ({@link MealLog}) can sum entries without recomputing
 * catalog math on every read. This is strictly additive: it never reads or writes a {@link
 * NutritionDayTemplate} or {@link MealTemplate} (spec FOR-127: "logging is additive — must NOT
 * mutate any plan template").
 *
 * <p><b>Known limitation (FOR-134).</b> {@code keyNutrients} is NOT persisted by {@code
 * JdbcMealLogRepository}: the {@code meal_log_entry} table (V13) has no key-nutrient columns, and
 * this story adds no migration (in-code reference data only, head stays V16). A JDBC round trip
 * therefore reconstructs {@link KeyNutrientTotals#empty()} for a reloaded entry even if the
 * original had known values — honest (never fabricated) rather than silently wrong. See {@code
 * JdbcMealLogRepositoryTest} and the FOR-134 PR's "Known limitations".
 *
 * @param date the day the meal was consumed
 * @param mealType the meal type; required
 * @param name human-readable name — the catalog food's name, or the free entry's provided name
 * @param foodItemId the FOR-30 catalog food id, or {@code null} for a free/ad-hoc entry
 * @param totals the entry's macro totals, computed once at logging time
 * @param keyNutrients the entry's key-nutrient totals (FOR-134), computed once at logging time;
 *     never {@code null} itself, but each of its four fields may be {@code null} (unknown)
 */
public record MealLogEntry(
    LocalDate date,
    MealType mealType,
    String name,
    String foodItemId,
    NutritionTotals totals,
    KeyNutrientTotals keyNutrients) {

  public MealLogEntry {
    Objects.requireNonNull(date, "date must not be null");
    Objects.requireNonNull(mealType, "mealType must not be null");
    Objects.requireNonNull(totals, "totals must not be null");
    Objects.requireNonNull(keyNutrients, "keyNutrients must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  /**
   * Convenience constructor for an entry with no known key nutrients (pre-FOR-134 call sites) —
   * {@link #keyNutrients} defaults to {@link KeyNutrientTotals#empty()}.
   */
  public MealLogEntry(
      LocalDate date, MealType mealType, String name, String foodItemId, NutritionTotals totals) {
    this(date, mealType, name, foodItemId, totals, KeyNutrientTotals.empty());
  }

  /**
   * Builds an entry from a resolved FOR-30 {@code food} and a portion count (number of the food's
   * {@link FoodItem#defaultServingG} servings). Macro math is delegated to {@link
   * NutritionCalculator#itemTotals} (FOR-32); key-nutrient math is delegated to {@link
   * NutritionCalculator#itemKeyNutrients} (FOR-134) — no duplicated formula here, both reuse the
   * same {@code quantityG}. The caller (application layer) is responsible for resolving {@code
   * foodItemId} to a {@link FoodItem} first, so an unknown id surfaces as a caller-input validation
   * error rather than a domain exception.
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
    MealItem item = new MealItem(food.id(), quantityG);
    NutritionTotals totals = NutritionCalculator.itemTotals(item);
    KeyNutrientTotals keyNutrients = NutritionCalculator.itemKeyNutrients(item);
    return new MealLogEntry(date, mealType, food.name(), food.id(), totals, keyNutrients);
  }

  /**
   * Builds a free/ad-hoc entry: macros are supplied directly, no catalog food is referenced, and no
   * key nutrients are known (spec FOR-134: free entries without key nutrients -> null).
   */
  public static MealLogEntry freeEntry(
      LocalDate date, MealType mealType, String name, NutritionTotals totals) {
    return freeEntry(date, mealType, name, totals, KeyNutrientTotals.empty());
  }

  /**
   * Builds a free/ad-hoc entry with optional key nutrients supplied directly by the caller
   * (FOR-134: "Free/ad-hoc meal entries may optionally provide key nutrients").
   */
  public static MealLogEntry freeEntry(
      LocalDate date,
      MealType mealType,
      String name,
      NutritionTotals totals,
      KeyNutrientTotals keyNutrients) {
    return new MealLogEntry(date, mealType, name, null, totals, keyNutrients);
  }
}

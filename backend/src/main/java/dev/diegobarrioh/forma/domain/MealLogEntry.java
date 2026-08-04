package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

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
 * <p><b>Persistence (FOR-134, V17).</b> {@code keyNutrients} is persisted alongside {@code totals}
 * by {@code JdbcMealLogRepository} into the V17 key-nutrient columns and read back on load, so it
 * survives a full round trip. Each nutrient is independently nullable: one a food genuinely lacks
 * is stored and reloaded as {@code null} ("unknown"), never fabricated as 0. See {@code
 * JdbcMealLogRepositoryTest}.
 *
 * @param date the day the meal was consumed
 * @param mealType the meal type; required
 * @param name human-readable name — the catalog food's name, or the free entry's provided name
 * @param foodItemId the FOR-30 catalog food id, or {@code null} for a free/ad-hoc entry
 * @param totals the entry's macro totals, computed once at logging time
 * @param keyNutrients the entry's key-nutrient totals (FOR-134), computed once at logging time;
 *     never {@code null} itself, but each of its four fields may be {@code null} (unknown)
 * @param plannedMealId which planned meal this was (V55), or {@code null} — which is the ordinary
 *     case and means "I ate this, and no plan said to". Not a status: whether a planned meal was
 *     eaten, skipped or still pending is answered by looking for entries pointing at it, and would
 *     go stale the moment it were stored
 */
public record MealLogEntry(
    LocalDate date,
    MealType mealType,
    String name,
    String foodItemId,
    NutritionTotals totals,
    KeyNutrientTotals keyNutrients,
    java.util.UUID plannedMealId) {

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
    this(date, mealType, name, foodItemId, totals, KeyNutrientTotals.empty(), null);
  }

  /**
   * Convenience constructor for an entry that no plan asked for, which is most of them (V55).
   *
   * <p>Kept so every call site written before plans were persisted still reads the same: an entry
   * without a planned meal is the ordinary case, not a special one.
   */
  public MealLogEntry(
      LocalDate date,
      MealType mealType,
      String name,
      String foodItemId,
      NutritionTotals totals,
      KeyNutrientTotals keyNutrients) {
    this(date, mealType, name, foodItemId, totals, keyNutrients, null);
  }

  /**
   * The same entry, recorded as having been the given planned meal (V55).
   *
   * <p>Attached after the entry is built rather than threaded through every factory: what was eaten
   * and which planned meal it answers are separate facts, and the macros do not change either way.
   */
  public MealLogEntry withPlannedMeal(java.util.UUID plannedMealId) {
    return new MealLogEntry(date, mealType, name, foodItemId, totals, keyNutrients, plannedMealId);
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
    if (food.defaultServingG() == null) {
      // Portions are defined as multiples of the food's serving; without one there is nothing to
      // multiply. Inventing a serving would silently invent the macros too — the caller logs this
      // food by grams instead.
      throw new IllegalArgumentException(
          "cannot log by portions: food has no defaultServingG: " + food.id());
    }
    int quantityG = (int) Math.round(portions * food.defaultServingG());
    if (quantityG <= 0) {
      quantityG = 1;
    }
    MealItem item = new MealItem(food.id(), quantityG);
    // The calculator resolves items through a lookup, but this entry is built FROM an already
    // resolved food: the only food it can possibly need is the one in hand, so that is the whole
    // lookup. No repository reaches this far into the domain.
    FoodLookup self = id -> Optional.of(food).filter(f -> f.id().equals(id));
    NutritionTotals totals = NutritionCalculator.itemTotals(item, self);
    KeyNutrientTotals keyNutrients = NutritionCalculator.itemKeyNutrients(item, self);
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

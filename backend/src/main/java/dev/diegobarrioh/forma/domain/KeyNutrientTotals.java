package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.function.Function;

/**
 * Key-nutrient totals (FOR-134): fibre, sugars, sodium and saturated fat for a single logged item
 * or a summed day. Companion to {@link NutritionTotals} (the FOR-32 macro totals) but every field
 * is independently nullable, because reference data (FOR-30 {@link FoodCatalog}) and free/ad-hoc
 * entries may not carry a given nutrient — {@code null} means "unknown", never a fabricated 0.
 *
 * <p><b>Units</b> (spec FOR-134, api.md): {@link #sodiumMg} is in milligrams (the conventional unit
 * for sodium); {@link #fiberG}, {@link #sugarsG} and {@link #saturatedFatG} are in grams.
 *
 * <p><b>Null/partial-total rule</b> (spec FOR-134 "Null / partial-total rule", documented
 * decision): when {@link #sum} combines several parts (e.g. a day's logged entries) into one total,
 * a nutrient's total is {@code null} if ANY contributing part lacks that nutrient. This is the
 * "Option A" choice from the spec — coarser than a silently-partial sum, but never misleads a
 * caller into treating an incomplete total as complete. This rule is applied consistently across
 * all four nutrients. The empty-parts case (no logged entries at all) is the one exception: it
 * totals to {@link #zero()}, mirroring {@link NutritionTotals}'s zeroed-empty-day behaviour, not
 * {@code null} (spec FOR-134 edge case: "Day with no logs → totals zero/null (per rule), never
 * 404").
 *
 * @param fiberG grams of fibre, or {@code null} if unknown; must be &gt;= 0 when present
 * @param sugarsG grams of sugars, or {@code null} if unknown; must be &gt;= 0 when present
 * @param sodiumMg milligrams of sodium, or {@code null} if unknown; must be &gt;= 0 when present
 * @param saturatedFatG grams of saturated fat, or {@code null} if unknown; must be &gt;= 0 when
 *     present
 */
public record KeyNutrientTotals(
    Double fiberG, Double sugarsG, Integer sodiumMg, Double saturatedFatG) {

  private static final KeyNutrientTotals EMPTY = new KeyNutrientTotals(null, null, null, null);
  private static final KeyNutrientTotals ZERO = new KeyNutrientTotals(0.0, 0.0, 0, 0.0);

  public KeyNutrientTotals {
    requireNonNegative(fiberG, "fiberG");
    requireNonNegative(sugarsG, "sugarsG");
    if (sodiumMg != null && sodiumMg < 0) {
      throw new IllegalArgumentException("sodiumMg must be >= 0, was: " + sodiumMg);
    }
    requireNonNegative(saturatedFatG, "saturatedFatG");
  }

  /** All four nutrients unknown — never fabricated. */
  public static KeyNutrientTotals empty() {
    return EMPTY;
  }

  /** All four nutrients zero — the "no contributing parts" case, distinct from unknown. */
  public static KeyNutrientTotals zero() {
    return ZERO;
  }

  /**
   * Sums {@code parts} into one total, applying the documented null/partial-total rule: a
   * nutrient's total is {@code null} if ANY part lacks it, independently per nutrient. An empty
   * {@code parts} list totals to {@link #zero()} (no entries logged yet is not "unknown data").
   */
  public static KeyNutrientTotals sum(List<KeyNutrientTotals> parts) {
    if (parts.isEmpty()) {
      return ZERO;
    }
    return new KeyNutrientTotals(
        sumGrams(parts, KeyNutrientTotals::fiberG),
        sumGrams(parts, KeyNutrientTotals::sugarsG),
        sumMilligrams(parts),
        sumGrams(parts, KeyNutrientTotals::saturatedFatG));
  }

  private static Double sumGrams(
      List<KeyNutrientTotals> parts, Function<KeyNutrientTotals, Double> field) {
    double sum = 0;
    for (KeyNutrientTotals part : parts) {
      Double value = field.apply(part);
      if (value == null) {
        return null;
      }
      sum += value;
    }
    return round1(sum);
  }

  private static Integer sumMilligrams(List<KeyNutrientTotals> parts) {
    int sum = 0;
    for (KeyNutrientTotals part : parts) {
      Integer value = part.sodiumMg();
      if (value == null) {
        return null;
      }
      sum += value;
    }
    return sum;
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private static void requireNonNegative(Double value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must be >= 0, was: " + value);
    }
  }
}

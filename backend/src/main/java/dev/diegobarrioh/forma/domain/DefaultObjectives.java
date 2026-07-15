package dev.diegobarrioh.forma.domain;

/**
 * The user's default objectives (FOR-107, spec FOR-58's Ajustes mockup): caloric deficit, protein
 * target and daily water target.
 *
 * <p>All three are optional presets (a user may not have set any of them yet), distinct from actual
 * per-day nutrition targets computed elsewhere (e.g. {@link NutritionCalculator}) — this is only
 * the user's stored default preference.
 *
 * @param caloricDeficitKcal default caloric deficit target in kcal/day; optional, non-negative when
 *     present
 * @param proteinTargetG default protein target in grams/day; optional, non-negative when present
 * @param dailyWaterMl default daily water target in milliliters; optional, non-negative when
 *     present
 */
public record DefaultObjectives(
    Double caloricDeficitKcal, Double proteinTargetG, Double dailyWaterMl) {

  public DefaultObjectives {
    requireNonNegative(caloricDeficitKcal, "caloricDeficitKcal");
    requireNonNegative(proteinTargetG, "proteinTargetG");
    requireNonNegative(dailyWaterMl, "dailyWaterMl");
  }

  /** No default objectives set, used before any preference has been saved. */
  public static final DefaultObjectives EMPTY = new DefaultObjectives(null, null, null);

  private static void requireNonNegative(Double value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative, was: " + value);
    }
  }
}

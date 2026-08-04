package dev.diegobarrioh.forma.domain;

/**
 * Whether computed totals reach a day's targets, per macro (FOR-32).
 *
 * <p>Descriptive only (reached / short per macro) — it prescribes no action; recommendations are
 * the Insights context (FOR-6), not here.
 *
 * @param caloriesReached day calories >= target calories
 * @param proteinReached day protein >= target protein
 * @param carbsReached day carbohydrates >= target carbohydrates
 * @param fatReached day fat >= target fat
 */
public record TargetComparison(
    boolean caloriesReached, boolean proteinReached, boolean carbsReached, boolean fatReached) {

  /**
   * Compares totals to a day's targets, or {@code null} when there is not a whole target to reach.
   *
   * <p>All four or nothing. A comparison that reported three macros and stayed silent on the fourth
   * would be read as "the fourth is fine", which is a different claim from "nobody set one" — and
   * {@link MacroTargets} exists precisely so those two can be told apart (FOR-134).
   */
  public static TargetComparison of(NutritionTotals totals, MacroTargets targets) {
    if (targets == null
        || targets.calories() == null
        || targets.proteinG() == null
        || targets.carbsG() == null
        || targets.fatG() == null) {
      return null;
    }
    return new TargetComparison(
        totals.calories() >= targets.calories(),
        totals.proteinG() >= targets.proteinG(),
        totals.carbsG() >= targets.carbsG(),
        totals.fatG() >= targets.fatG());
  }
}

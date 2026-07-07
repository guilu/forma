package dev.diegobarrioh.forma.domain;

/**
 * Whether computed day totals reach a {@link NutritionDayTemplate}'s targets, per macro (FOR-32).
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

  /** Compares day totals to a day template's targets. */
  public static TargetComparison of(NutritionTotals totals, NutritionDayTemplate target) {
    return new TargetComparison(
        totals.calories() >= target.targetCalories(),
        totals.proteinG() >= target.targetProteinG(),
        totals.carbsG() >= target.targetCarbsG(),
        totals.fatG() >= target.targetFatG());
  }
}

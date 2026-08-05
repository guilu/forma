package dev.diegobarrioh.forma.domain;

import java.util.Objects;

/**
 * How many calories a day somebody needs, and the three steps that get there.
 *
 * <p>Pure and framework-free (ADR-001), and it lives here rather than in the funnel's React because
 * the plan generator needs the same arithmetic. Two copies of Mifflin-St Jeor would be free to
 * disagree the first time anybody touched a coefficient, and the number shown to somebody deciding
 * whether to trust the product would stop matching the one their plan was built from. That is the
 * fault this codebase has spent fourteen migrations removing.
 *
 * <p>The three figures are kept apart because the funnel shows them apart, and because they answer
 * different questions:
 *
 * <pre>
 *   GEB (basal)             1668 kcal   lo que gasta el cuerpo en reposo
 *   × factor actividad       × 1,55     lo que añade moverse
 *   GET (total)             2585 kcal   lo que gasta al día
 *   × ajuste por objetivo    × 0,80     lo que se decide comer
 *   Requerimiento del plan  2068 kcal
 * </pre>
 *
 * @param basalKcal the Mifflin-St Jeor resting requirement
 * @param activityFactor what movement multiplies it by
 * @param dailyKcal basal × activity: what the body spends
 * @param objectiveFactor what the goal multiplies THAT by
 * @param planKcal what the plan is built to deliver
 */
public record EnergyRequirement(
    int basalKcal, double activityFactor, int dailyKcal, double objectiveFactor, int planKcal) {

  /**
   * Works out the requirement from what somebody can tell you about themselves.
   *
   * <p>Mifflin-St Jeor, which is {@code 10·kg + 6,25·cm − 5·años} plus 5 for men and minus 161 for
   * women. It is the formula the funnel names on screen, and naming it is the point: a number with
   * a source is worth more than a number.
   *
   * <p>Rounded ONCE, at each figure the screen shows, from the unrounded one before it. Rounding
   * the basal figure and then multiplying would make the total drift from what the same inputs give
   * a calculator anywhere else — the same discipline {@code NutritionCalculator} follows when it
   * sums raw contributions and rounds at the end.
   *
   * @param sex required; {@link Sex#OTHER} has no coefficient in this formula — see below
   * @param ageYears must be strictly positive
   * @param weightKg must be strictly positive
   * @param heightCm must be strictly positive
   */
  public static EnergyRequirement of(
      Sex sex,
      int ageYears,
      double weightKg,
      double heightCm,
      ActivityLevel activity,
      PlanObjective objective) {
    Objects.requireNonNull(sex, "sex must not be null");
    Objects.requireNonNull(activity, "activity must not be null");
    Objects.requireNonNull(objective, "objective must not be null");
    requirePositive(ageYears, "ageYears");
    requirePositive(weightKg, "weightKg");
    requirePositive(heightCm, "heightCm");

    double basal = 10 * weightKg + 6.25 * heightCm - 5.0 * ageYears + sexOffset(sex);
    double daily = basal * activity.factor();
    double plan = daily * objective.factor();
    return new EnergyRequirement(
        (int) Math.round(basal),
        activity.factor(),
        (int) Math.round(daily),
        objective.factor(),
        (int) Math.round(plan));
  }

  /**
   * The constant Mifflin-St Jeor adds, by sex.
   *
   * <p>{@link Sex#OTHER} takes the midpoint of the two, and that is a decision rather than a
   * derivation: the formula was fitted on two groups and defines nothing else, so the options are
   * to refuse the calculation, pick one, or sit between them. Refusing would leave somebody unable
   * to use the funnel at all; picking one would decide something about them that they did not. The
   * midpoint is the least wrong of the three and it is written down here rather than hidden.
   */
  private static double sexOffset(Sex sex) {
    return switch (sex) {
      case MALE -> 5.0;
      case FEMALE -> -161.0;
      case OTHER -> -78.0;
    };
  }

  private static void requirePositive(double value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be strictly positive, was: " + value);
    }
  }
}

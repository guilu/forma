package dev.diegobarrioh.forma.domain;

/**
 * The user's personal plan targets (FOR-149, epic FOR-148 slice 1, sourced from the *Perfil*
 * sheet): base daily calories, body-fat and weight target ranges, and fat/carbohydrate macro
 * targets.
 *
 * <p>Protein deliberately stays out of this record: the sheet's protein target (160 g/day for
 * Diego) reuses the existing {@link DefaultObjectives#proteinTargetG()} field/column rather than
 * duplicating it (spec FOR-149 Functional Requirements: "Reuse the existing protein_target_g column
 * for protein"). This record only adds the fields {@link DefaultObjectives} does not have.
 *
 * <p>All fields are optional so an unseeded/partial profile is still a valid aggregate (spec
 * FOR-149 Edge Cases). Range fields additionally require {@code min <= max} when both bounds are
 * present.
 *
 * @param baseCaloriesKcal base daily calorie target in kcal/day; optional, non-negative when
 *     present
 * @param bodyFatTargetMinPct lower bound of the target body-fat percentage range; optional,
 *     non-negative when present
 * @param bodyFatTargetMaxPct upper bound of the target body-fat percentage range; optional,
 *     non-negative when present, must be {@code >=} {@link #bodyFatTargetMinPct()} when both are
 *     set
 * @param weightTargetMinKg lower bound of the target weight range in kilograms; optional,
 *     non-negative when present
 * @param weightTargetMaxKg upper bound of the target weight range in kilograms; optional,
 *     non-negative when present, must be {@code >=} {@link #weightTargetMinKg()} when both are set
 * @param fatTargetG daily fat macro target in grams/day; optional, non-negative when present
 * @param carbsTargetG daily carbohydrate macro target in grams/day; optional, non-negative when
 *     present
 */
public record PersonalTargets(
    Double baseCaloriesKcal,
    Double bodyFatTargetMinPct,
    Double bodyFatTargetMaxPct,
    Double weightTargetMinKg,
    Double weightTargetMaxKg,
    Double fatTargetG,
    Double carbsTargetG) {

  public PersonalTargets {
    requireNonNegative(baseCaloriesKcal, "baseCaloriesKcal");
    requireNonNegative(bodyFatTargetMinPct, "bodyFatTargetMinPct");
    requireNonNegative(bodyFatTargetMaxPct, "bodyFatTargetMaxPct");
    requireNonNegative(weightTargetMinKg, "weightTargetMinKg");
    requireNonNegative(weightTargetMaxKg, "weightTargetMaxKg");
    requireNonNegative(fatTargetG, "fatTargetG");
    requireNonNegative(carbsTargetG, "carbsTargetG");
    requireRange(bodyFatTargetMinPct, bodyFatTargetMaxPct, "bodyFatTarget");
    requireRange(weightTargetMinKg, weightTargetMaxKg, "weightTarget");
  }

  /** No personal targets set, used before any personal-plan data has been seeded/saved. */
  public static final PersonalTargets EMPTY =
      new PersonalTargets(null, null, null, null, null, null, null);

  private static void requireNonNegative(Double value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative, was: " + value);
    }
  }

  private static void requireRange(Double min, Double max, String field) {
    if (min != null && max != null && min > max) {
      throw new IllegalArgumentException(
          field + " range is invalid: min (" + min + ") must not exceed max (" + max + ")");
    }
  }
}

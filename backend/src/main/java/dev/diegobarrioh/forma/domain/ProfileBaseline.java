package dev.diegobarrioh.forma.domain;

/**
 * The profile's plan reference baseline (FOR-149, epic FOR-148 slice 1): the "peso/grasa/IMC
 * iniciales" (initial weight/body-fat/BMI) captured once from the *Perfil* sheet when the personal
 * plan targets were set.
 *
 * <p>This is deliberately <b>not</b> a {@code body_measurement} row: it is a fixed plan reference
 * point on the profile itself (spec FOR-149 Data Model Notes), seeded once as reference data. Real
 * tracked measurements over time (SEGUIMIENTO) stay in the separate, still-empty {@code
 * body_measurement} table until slice 7 (FOR-155) starts populating it.
 *
 * <p>All three values are optional so an unseeded profile is still a valid aggregate (spec FOR-149
 * Edge Cases); {@code bmi} is stored verbatim from the sheet rather than (re)computed here — this
 * value object carries data, not calculation (ADR-001: framework-free, rule-free domain data holder
 * for this slice).
 *
 * @param weightKg baseline body weight in kilograms; optional, non-negative when present
 * @param bodyFatPct baseline body-fat percentage; optional, non-negative when present
 * @param bmi baseline body mass index, sourced verbatim from the *Perfil* sheet; optional,
 *     non-negative when present
 */
public record ProfileBaseline(Double weightKg, Double bodyFatPct, Double bmi) {

  public ProfileBaseline {
    requireNonNegative(weightKg, "weightKg");
    requireNonNegative(bodyFatPct, "bodyFatPct");
    requireNonNegative(bmi, "bmi");
  }

  /** No baseline captured yet, used before any personal-plan data has been seeded/saved. */
  public static final ProfileBaseline EMPTY = new ProfileBaseline(null, null, null);

  private static void requireNonNegative(Double value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative, was: " + value);
    }
  }
}

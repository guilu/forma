package dev.diegobarrioh.forma.application;

/**
 * Section 11's tolerance band (ADR-015 slice 4): how far a plan's claimed {@code target_*} may sit
 * from what {@link PlanToleranceAuditor} recomputes before it counts as drift rather than rounding.
 *
 * <p>{@link #SECTION_11_DEFAULTS} is quoted verbatim from {@code
 * docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md}, section 11:
 *
 * <pre>
 *   Calorías: ±5 %
 *   Proteína: ±5 g
 *   Carbohidratos: ±10 g
 *   Grasas: ±5 g
 * </pre>
 *
 * <p>Kcal is a percentage of the claimed value because the source document states it as one;
 * protein, carbs and fat are flat gram bands, also as stated. A record rather than a set of
 * constants because ADR-015 decision 9 calls the tolerances "configurable" — this is the seam a
 * future {@code @ConfigurationProperties} can construct a different instance from, without touching
 * {@link PlanToleranceAuditor}.
 *
 * @param kcalPercentage fraction of the claimed kcal value allowed either side, e.g. {@code 0.05}
 *     for ±5&nbsp;%
 * @param proteinGrams flat grams of protein allowed either side of the claimed value
 * @param carbsGrams flat grams of carbohydrate allowed either side of the claimed value
 * @param fatGrams flat grams of fat allowed either side of the claimed value
 */
public record ToleranceThresholds(
    double kcalPercentage, double proteinGrams, double carbsGrams, double fatGrams) {

  /** Section 11's own example tolerances, unless something more specific is configured. */
  public static final ToleranceThresholds SECTION_11_DEFAULTS =
      new ToleranceThresholds(0.05, 5.0, 10.0, 5.0);

  public ToleranceThresholds {
    if (kcalPercentage <= 0) {
      throw new IllegalArgumentException("kcalPercentage must be > 0, was: " + kcalPercentage);
    }
    if (proteinGrams <= 0) {
      throw new IllegalArgumentException("proteinGrams must be > 0, was: " + proteinGrams);
    }
    if (carbsGrams <= 0) {
      throw new IllegalArgumentException("carbsGrams must be > 0, was: " + carbsGrams);
    }
    if (fatGrams <= 0) {
      throw new IllegalArgumentException("fatGrams must be > 0, was: " + fatGrams);
    }
  }
}

package dev.diegobarrioh.forma.domain;

/**
 * Descriptive BMI band for a {@link BodyMeasurement#bmi()} value (FOR-101).
 *
 * <p>Pure, framework-free domain classifier (ADR-001): maps an already-computed BMI value onto one
 * of four WHO adult reference bands so the Mediciones UI (FOR-52) can render a neutral "Saludable"
 * style badge without hardcoding thresholds. This does <b>not</b> compute BMI (FOR-15's {@code bmi}
 * is either client-supplied or absent) and it is a descriptive label only — never medical advice or
 * a diagnosis (docs/ui-guidelines.md).
 *
 * <h2>Bands (bounds documented explicitly, spec FOR-101 Data Model Notes / api.md)</h2>
 *
 * <ul>
 *   <li>{@link #BAJO_PESO} — {@code bmi < }{@link #BAJO_PESO_MAX}
 *   <li>{@link #SALUDABLE} — {@link #BAJO_PESO_MAX} {@code <= bmi < }{@link #SALUDABLE_MAX}
 *   <li>{@link #SOBREPESO} — {@link #SALUDABLE_MAX} {@code <= bmi < }{@link #SOBREPESO_MAX}
 *   <li>{@link #OBESIDAD} — {@code bmi >= }{@link #SOBREPESO_MAX}
 * </ul>
 *
 * <p>Every lower bound is inclusive and every upper bound is exclusive, so a boundary value (18.5,
 * 25.0 or 30.0) always lands in exactly one band (spec FOR-101 Edge Cases) — never two, never zero.
 */
public enum BmiCategory {
  BAJO_PESO,
  SALUDABLE,
  SOBREPESO,
  OBESIDAD;

  /** Exclusive upper bound of {@link #BAJO_PESO}; inclusive lower bound of {@link #SALUDABLE}. */
  public static final double BAJO_PESO_MAX = 18.5;

  /** Exclusive upper bound of {@link #SALUDABLE}; inclusive lower bound of {@link #SOBREPESO}. */
  public static final double SALUDABLE_MAX = 25.0;

  /** Exclusive upper bound of {@link #SOBREPESO}; inclusive lower bound of {@link #OBESIDAD}. */
  public static final double SOBREPESO_MAX = 30.0;

  /**
   * Classifies a BMI value into its descriptive band.
   *
   * @param bmi the measurement's BMI, or {@code null} when not recorded
   * @return the matching band, or {@code null} when {@code bmi} is {@code null} — a missing BMI
   *     never fabricates a category (spec FOR-101 Edge Cases)
   */
  public static BmiCategory classify(Double bmi) {
    if (bmi == null) {
      return null;
    }
    if (bmi < BAJO_PESO_MAX) {
      return BAJO_PESO;
    }
    if (bmi < SALUDABLE_MAX) {
      return SALUDABLE;
    }
    if (bmi < SOBREPESO_MAX) {
      return SOBREPESO;
    }
    return OBESIDAD;
  }
}

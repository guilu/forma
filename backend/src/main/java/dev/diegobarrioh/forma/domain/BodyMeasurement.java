package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A single body-composition measurement event (FOR-15).
 *
 * <p>This is the Body bounded context's core domain type (docs/domain-model.md). It is
 * framework-free: no Spring, JPA/JDBC or HTTP types (ADR-001). Persistence (FOR-16), API (FOR-17)
 * and UI (FOR-18/19/20) are added by later stories on top of this type.
 *
 * <p>Derived mass values are computed on demand from {@code weightKg} and {@code bodyFatPercentage}
 * so there is a single source of truth and no rounding drift between {@link #fatMassKg()} and
 * {@link #leanMassKg()}:
 *
 * <pre>
 *   fatMassKg  = weightKg * bodyFatPercentage / 100
 *   leanMassKg = weightKg - fatMassKg
 * </pre>
 *
 * <p>When {@code bodyFatPercentage} is absent, the derived values cannot be computed and both
 * accessors return {@link Optional#empty()} rather than a misleading {@code 0} (spec FOR-15 Edge
 * Cases).
 *
 * <p>Validation happens at construction to keep the type internally consistent (spec FOR-15 Open
 * Questions). The API layer (FOR-17) adds user-facing validation messages on top of this.
 *
 * @param measuredAt when the measurement was taken; required
 * @param source how the measurement originated; required (see {@link MeasurementSource})
 * @param weightKg body weight in kilograms; must be strictly positive
 * @param bodyFatPercentage body fat as a percentage in {@code [0, 100]}, or {@code null} if unknown
 * @param bmi body mass index if provided, or {@code null}; not derived here (no height input)
 * @param notes optional free-text note; never affects calculation
 */
public record BodyMeasurement(
    Instant measuredAt,
    MeasurementSource source,
    double weightKg,
    Double bodyFatPercentage,
    Double bmi,
    String notes) {

  public BodyMeasurement {
    Objects.requireNonNull(measuredAt, "measuredAt must not be null");
    Objects.requireNonNull(source, "source must not be null");
    if (weightKg <= 0) {
      throw new IllegalArgumentException("weightKg must be strictly positive, was: " + weightKg);
    }
    if (bodyFatPercentage != null && (bodyFatPercentage < 0 || bodyFatPercentage > 100)) {
      throw new IllegalArgumentException(
          "bodyFatPercentage must be within [0, 100], was: " + bodyFatPercentage);
    }
  }

  /**
   * Fat mass in kilograms, derived as {@code weightKg * bodyFatPercentage / 100}.
   *
   * @return the derived fat mass, or empty when {@code bodyFatPercentage} is absent
   */
  public Optional<Double> fatMassKg() {
    if (bodyFatPercentage == null) {
      return Optional.empty();
    }
    return Optional.of(weightKg * bodyFatPercentage / 100);
  }

  /**
   * Lean mass in kilograms, derived as {@code weightKg - fatMassKg}.
   *
   * @return the derived lean mass, or empty when {@code bodyFatPercentage} is absent
   */
  public Optional<Double> leanMassKg() {
    return fatMassKg().map(fatMass -> weightKg - fatMass);
  }
}

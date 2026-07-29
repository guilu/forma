package dev.diegobarrioh.forma.delivery.body;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.StoredBodyMeasurement;
import dev.diegobarrioh.forma.domain.BmiCategory;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import java.time.Instant;

/**
 * Response body for the body measurements API (FOR-17), used by both {@code GET} (list items) and
 * {@code POST} (created resource).
 *
 * <p>A delivery-layer read model, distinct from the FOR-15 domain type and FOR-16 persistence row
 * (ADR-005). Derived {@code fatMassKg}/{@code leanMassKg} come straight from the domain type — they
 * are never recomputed here. They are omitted from the JSON when absent (no {@code
 * bodyFatPercentage}); null fields are dropped like the {@link
 * dev.diegobarrioh.forma.delivery.error.ApiError} shape.
 *
 * <p>{@code bmiCategory} (FOR-101) is derived on read from {@code bmi} via the pure {@link
 * BmiCategory#classify(Double)} domain classifier — never persisted, never recomputed from anything
 * but the existing {@code bmi} — and serialized as its enum name (e.g. {@code "SALUDABLE"}),
 * following the {@code category}/{@code severity} convention in {@code WeeklyInsightsResponse}.
 * Omitted from the JSON when {@code bmi} is absent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BodyMeasurementResponse(
    String id,
    Instant measuredAt,
    String source,
    double weightKg,
    Double bodyFatPercentage,
    Double bmi,
    String bmiCategory,
    Double fatMassKg,
    Double leanMassKg,
    Double muscleMassKg,
    Double waterPercentage,
    String notes) {

  /**
   * Maps a stored measurement — id included — to its API read model (FOR-187). This is what {@code
   * GET} returns, and the id is what {@code DELETE /{id}} addresses.
   */
  public static BodyMeasurementResponse from(StoredBodyMeasurement stored) {
    return from(stored.measurement(), stored.id().toString());
  }

  /**
   * Maps a measurement with no stored row behind it yet: the {@code POST} response, whose id the
   * create use case does not return (the row's key is generated in the persistence adapter). {@code
   * id} is then absent from the JSON rather than null or invented — a client that needs it re-reads
   * the list.
   */
  public static BodyMeasurementResponse from(BodyMeasurement measurement) {
    return from(measurement, null);
  }

  private static BodyMeasurementResponse from(BodyMeasurement measurement, String id) {
    BmiCategory category = BmiCategory.classify(measurement.bmi());
    return new BodyMeasurementResponse(
        id,
        measurement.measuredAt(),
        measurement.source().name(),
        measurement.weightKg(),
        measurement.bodyFatPercentage(),
        measurement.bmi(),
        category == null ? null : category.name(),
        measurement.fatMassKg().orElse(null),
        measurement.leanMassKg().orElse(null),
        measurement.muscleMassKg(),
        measurement.waterPercentage(),
        measurement.notes());
  }
}

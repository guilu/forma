package dev.diegobarrioh.forma.delivery.body;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BodyMeasurementResponse(
    Instant measuredAt,
    String source,
    double weightKg,
    Double bodyFatPercentage,
    Double bmi,
    Double fatMassKg,
    Double leanMassKg,
    Double muscleMassKg,
    Double waterPercentage,
    String notes) {

  /** Maps a domain measurement to its API read model. */
  public static BodyMeasurementResponse from(BodyMeasurement measurement) {
    return new BodyMeasurementResponse(
        measurement.measuredAt(),
        measurement.source().name(),
        measurement.weightKg(),
        measurement.bodyFatPercentage(),
        measurement.bmi(),
        measurement.fatMassKg().orElse(null),
        measurement.leanMassKg().orElse(null),
        measurement.muscleMassKg(),
        measurement.waterPercentage(),
        measurement.notes());
  }
}

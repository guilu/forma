package dev.diegobarrioh.forma.delivery.tracking;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.time.LocalDate;

/**
 * Response body for the weekly tracking record API (FOR-155), used by list/create/read.
 *
 * <p>A delivery-layer read model, distinct from the FOR-155 domain type and persistence row
 * (ADR-005). {@code fatMassKg}/{@code leanMassKg} come straight from the domain type — they are
 * never recomputed here — and are omitted from the JSON when absent, mirroring {@code
 * BodyMeasurementResponse}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeeklyTrackingRecordResponse(
    int week,
    LocalDate date,
    Double weightKg,
    Double bodyFatPercentage,
    Double fatMassKg,
    Double leanMassKg,
    Double bmi,
    Double runningKm,
    String pace4kmMinPerKm,
    Double recommendedKcal,
    String comment) {

  /** Maps a domain weekly tracking record to its API read model. */
  public static WeeklyTrackingRecordResponse from(WeeklyTrackingRecord record) {
    return new WeeklyTrackingRecordResponse(
        record.week(),
        record.date(),
        record.weightKg(),
        record.bodyFatPercentage(),
        record.fatMassKg().orElse(null),
        record.leanMassKg().orElse(null),
        record.bmi(),
        record.runningKm(),
        record.pace4kmMinPerKm(),
        record.recommendedKcal(),
        record.comment());
  }
}

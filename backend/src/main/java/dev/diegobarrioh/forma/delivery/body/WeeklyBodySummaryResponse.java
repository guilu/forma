package dev.diegobarrioh.forma.delivery.body;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;

/**
 * Response body for {@code GET /api/v1/body/weekly-summary} (FOR-97).
 *
 * <p>Delivery read model, distinct from the FOR-21 domain {@link WeeklyBodySummary} record (ADR-005
 * — controllers never return domain types directly). Carries the latest weight/body fat/lean mass,
 * weekly deltas, {@code comparisonDays} and the message as-is; no recomputation.
 *
 * <p>Honesty rules (FOR-21): delta fields and {@code comparisonDays} are {@code null} when there
 * are fewer than two measurements, never {@code 0}. Null fields are omitted from the JSON, the same
 * {@code @JsonInclude(NON_NULL)} convention as {@link BodyMeasurementResponse}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeeklyBodySummaryResponse(
    Double latestWeightKg,
    Double latestBodyFatPercentage,
    Double latestLeanMassKg,
    Double weeklyWeightChangeKg,
    Double weeklyBodyFatChange,
    Integer comparisonDays,
    String message) {

  /** Maps the computed summary to its API read model. */
  public static WeeklyBodySummaryResponse from(WeeklyBodySummary summary) {
    return new WeeklyBodySummaryResponse(
        summary.latestWeightKg(),
        summary.latestBodyFatPercentage(),
        summary.latestLeanMassKg(),
        summary.weeklyWeightChangeKg(),
        summary.weeklyBodyFatChange(),
        summary.comparisonDays(),
        summary.message());
  }
}

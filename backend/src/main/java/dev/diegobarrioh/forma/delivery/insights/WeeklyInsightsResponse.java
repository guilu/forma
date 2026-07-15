package dev.diegobarrioh.forma.delivery.insights;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import dev.diegobarrioh.forma.domain.WeeklyCheckInDeltas;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/insights/weekly} (FOR-45) and each entry of {@code GET
 * /api/v1/insights/history} (FOR-110): the weekly check-in summary (its {@code weekStartDate} is
 * the period), the main recommendation, any secondary recommendations, the generated timestamp, and
 * the week-over-week {@link Deltas}.
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyInsights} and the domain types
 * (ADR-005 — controllers never return application/domain types directly). Null body values and a
 * null {@code relatedMetric}/{@code notes} are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeeklyInsightsResponse(
    CheckIn checkIn,
    Recommendation main,
    List<Recommendation> secondary,
    Instant generatedAt,
    Deltas deltas) {

  /** The FOR-40 snapshot; absent body values are omitted. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CheckIn(
      LocalDate weekStartDate,
      Double latestWeightKg,
      Double latestBodyFatPercentage,
      Double latestLeanMassKg,
      int plannedRunningSessions,
      int completedRunningSessions,
      int plannedStrengthSessions,
      int completedStrengthSessions,
      String notes) {}

  /** An explainable recommendation; {@code relatedMetric} is omitted when absent. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Recommendation(
      String category,
      String severity,
      String message,
      String reason,
      String relatedMetric,
      Instant createdAt) {}

  /**
   * Week-over-week deltas (FOR-110) vs. the immediately prior persisted period; each field is
   * omitted (not {@code 0}) when there is no prior period or the underlying value is missing on
   * either side.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Deltas(
      Double weightDeltaKg,
      Double bodyFatPercentageDelta,
      Double leanMassDeltaKg,
      Integer trainingCompletionDelta) {

    static Deltas from(WeeklyCheckInDeltas deltas) {
      return new Deltas(
          deltas.weightDeltaKg(),
          deltas.bodyFatPercentageDelta(),
          deltas.leanMassDeltaKg(),
          deltas.trainingCompletionDelta());
    }
  }

  /** Maps the assembled insights plus its computed deltas to the API read model. */
  public static WeeklyInsightsResponse from(WeeklyInsights insights, WeeklyCheckInDeltas deltas) {
    return new WeeklyInsightsResponse(
        checkIn(insights.checkIn()),
        toView(insights.main()),
        insights.secondary().stream().map(WeeklyInsightsResponse::toView).toList(),
        insights.generatedAt(),
        Deltas.from(deltas));
  }

  private static CheckIn checkIn(WeeklyCheckIn checkIn) {
    return new CheckIn(
        checkIn.weekStartDate(),
        checkIn.latestWeightKg(),
        checkIn.latestBodyFatPercentage(),
        checkIn.latestLeanMassKg(),
        checkIn.plannedRunningSessions(),
        checkIn.completedRunningSessions(),
        checkIn.plannedStrengthSessions(),
        checkIn.completedStrengthSessions(),
        checkIn.notes());
  }

  private static Recommendation toView(dev.diegobarrioh.forma.domain.Recommendation rec) {
    return new Recommendation(
        rec.category().name(),
        rec.severity().name(),
        rec.message(),
        rec.reason(),
        rec.relatedMetric(),
        rec.createdAt());
  }
}

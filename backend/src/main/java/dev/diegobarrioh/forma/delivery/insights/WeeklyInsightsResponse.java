package dev.diegobarrioh.forma.delivery.insights;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/insights/weekly} (FOR-45): the weekly check-in summary, the
 * main recommendation, any secondary recommendations, and the generated timestamp.
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyInsights} and the domain types
 * (ADR-005 — controllers never return application/domain types directly). Null body values and a
 * null {@code relatedMetric}/{@code notes} are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeeklyInsightsResponse(
    CheckIn checkIn, Recommendation main, List<Recommendation> secondary, Instant generatedAt) {

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

  /** Maps the assembled insights to the API read model. */
  public static WeeklyInsightsResponse from(WeeklyInsights insights) {
    return new WeeklyInsightsResponse(
        checkIn(insights.checkIn()),
        toView(insights.main()),
        insights.secondary().stream().map(WeeklyInsightsResponse::toView).toList(),
        insights.generatedAt());
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

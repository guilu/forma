package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.WeeklyTrainingSummary;

/**
 * Response body for {@code GET /api/v1/training/weekly-summary} (FOR-98).
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyTrainingSummary} (ADR-005 —
 * controllers never return application/domain types directly). Carries the FOR-28 counts,
 * planned/completed running distance and the summary message as-is; no recomputation.
 */
public record WeeklyTrainingSummaryResponse(
    int plannedRunningSessions,
    int completedRunningSessions,
    int plannedStrengthSessions,
    int completedStrengthSessions,
    double totalPlannedRunningKm,
    double completedRunningKm,
    String message) {

  /** Maps the computed summary to its API read model. */
  public static WeeklyTrainingSummaryResponse from(WeeklyTrainingSummary summary) {
    return new WeeklyTrainingSummaryResponse(
        summary.plannedRunningSessions(),
        summary.completedRunningSessions(),
        summary.plannedStrengthSessions(),
        summary.completedStrengthSessions(),
        summary.totalPlannedRunningKm(),
        summary.completedRunningKm(),
        summary.message());
  }
}

package dev.diegobarrioh.forma.application;

/**
 * A simple weekly training adherence summary (FOR-28): planned vs. completed running and strength
 * sessions, and planned vs. completed running distance, for the current week.
 *
 * <p>Application-layer read model computed on demand by {@link WeeklyTrainingSummaryService} from
 * the FOR-26 schedule and FOR-27 completion status. Counts and sums only — no forecasting.
 * Completed running distance reflects only sessions marked completed. Distances are in kilometers,
 * rounded to one decimal.
 *
 * @param plannedRunningSessions running sessions planned this week
 * @param completedRunningSessions running sessions marked completed
 * @param plannedStrengthSessions strength sessions planned this week
 * @param completedStrengthSessions strength sessions marked completed
 * @param totalPlannedRunningKm total planned running distance
 * @param completedRunningKm running distance from completed sessions only
 * @param message short, factual human-readable summary
 */
public record WeeklyTrainingSummary(
    int plannedRunningSessions,
    int completedRunningSessions,
    int plannedStrengthSessions,
    int completedStrengthSessions,
    double totalPlannedRunningKm,
    double completedRunningKm,
    String message) {}

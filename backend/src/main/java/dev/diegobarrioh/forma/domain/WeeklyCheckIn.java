package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A snapshot of the user's week for the Insights engine (FOR-40): the latest body-composition
 * values plus planned/completed training counts. Framework-free domain value (ADR-001), consumed by
 * the FOR-42/43/44 rules and exposed by FOR-45.
 *
 * <p>This is the Insights-context check-in, distinct from the Body-context {@link
 * WeeklyBodySummary} (FOR-21) and the Training-context {@code WeeklyTrainingSummary} (FOR-28) that
 * feed it. It is <em>assembled</em> from those existing summaries — it never recomputes body or
 * training metrics.
 *
 * <p>Honesty rules (spec FOR-40):
 *
 * <ul>
 *   <li>Body values are {@code null} (not fabricated) when there are no measurements — carried from
 *       {@link WeeklyBodySummary} as-is, no fake precision.
 *   <li>Session counts are {@code 0} when nothing is planned/completed, mirroring the training
 *       summary.
 *   <li>Nutrition and shopping are out of scope for this iteration and are not represented.
 * </ul>
 *
 * <p>Weekly body <em>deltas</em> (weekly weight/body-fat change) needed by the FOR-42 rules are not
 * duplicated here; those rules read them from {@link WeeklyBodySummary} (FOR-21) directly. This
 * keeps the check-in a single source of the snapshot values without carrying derived fields that
 * would drift from their source.
 *
 * @param weekStartDate the Monday (or chosen start) of the summarized week; required
 * @param latestWeightKg most recent weight, or null if there are no measurements
 * @param latestBodyFatPercentage most recent body fat %, or null
 * @param latestLeanMassKg most recent lean mass, or null
 * @param plannedRunningSessions running sessions planned this week
 * @param completedRunningSessions running sessions marked completed
 * @param plannedStrengthSessions strength sessions planned this week
 * @param completedStrengthSessions strength sessions marked completed
 * @param notes optional, manually supplied notes; may be null
 */
public record WeeklyCheckIn(
    LocalDate weekStartDate,
    Double latestWeightKg,
    Double latestBodyFatPercentage,
    Double latestLeanMassKg,
    int plannedRunningSessions,
    int completedRunningSessions,
    int plannedStrengthSessions,
    int completedStrengthSessions,
    String notes) {

  public WeeklyCheckIn {
    Objects.requireNonNull(weekStartDate, "weekStartDate must not be null");
  }
}

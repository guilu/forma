package dev.diegobarrioh.forma.domain;

import java.util.Objects;

/**
 * Week-over-week deltas between two {@link WeeklyCheckIn} snapshots (FOR-110): weight, body fat %,
 * lean mass and training-completion-count change vs. the immediately prior persisted period.
 * Framework-free domain value (ADR-001), computed by {@link #between(WeeklyCheckIn, WeeklyCheckIn)}
 * and surfaced on the FOR-45 insights response.
 *
 * <p>Honesty rule (spec FOR-110 Edge Cases): a field is {@code null} whenever either side of the
 * comparison is missing — never fabricated as {@code 0} — so the frontend can distinguish "no
 * change" from "no data to compare". This mirrors {@link WeeklyBodySummary}'s existing
 * null-not-zero convention for its own week-over-week fields.
 *
 * @param weightDeltaKg current vs. prior latest weight, or null if either side is missing
 * @param bodyFatPercentageDelta current vs. prior latest body fat %, or null if either side is
 *     missing
 * @param leanMassDeltaKg current vs. prior latest lean mass, or null if either side is missing
 * @param trainingCompletionDelta current vs. prior total completed sessions (running + strength),
 *     or null if there is no prior period at all
 */
public record WeeklyCheckInDeltas(
    Double weightDeltaKg,
    Double bodyFatPercentageDelta,
    Double leanMassDeltaKg,
    Integer trainingCompletionDelta) {

  /** No prior period to compare against (e.g. the first-ever generated week). */
  public static final WeeklyCheckInDeltas NONE = new WeeklyCheckInDeltas(null, null, null, null);

  /**
   * Computes the deltas of {@code current} vs. {@code prior}. When {@code prior} is {@code null}
   * (no earlier persisted period exists), returns {@link #NONE} rather than fabricating zeros.
   */
  public static WeeklyCheckInDeltas between(WeeklyCheckIn current, WeeklyCheckIn prior) {
    Objects.requireNonNull(current, "current must not be null");
    if (prior == null) {
      return NONE;
    }
    return new WeeklyCheckInDeltas(
        delta(current.latestWeightKg(), prior.latestWeightKg()),
        delta(current.latestBodyFatPercentage(), prior.latestBodyFatPercentage()),
        delta(current.latestLeanMassKg(), prior.latestLeanMassKg()),
        totalCompletedSessions(current) - totalCompletedSessions(prior));
  }

  private static Double delta(Double current, Double prior) {
    return (current == null || prior == null) ? null : current - prior;
  }

  private static int totalCompletedSessions(WeeklyCheckIn checkIn) {
    return checkIn.completedRunningSessions() + checkIn.completedStrengthSessions();
  }
}

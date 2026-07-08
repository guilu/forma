package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.LocalDate;

/**
 * Assembles a {@link WeeklyCheckIn} (FOR-40) from the existing FOR-21 {@link WeeklyBodySummary} and
 * FOR-28 {@link WeeklyTrainingSummary}.
 *
 * <p>Lives in the application layer because it composes a domain summary ({@link
 * WeeklyBodySummary}) with an application read model ({@code WeeklyTrainingSummary}); the domain
 * {@link WeeklyCheckIn} must not depend on the application layer.
 *
 * <p>Degrades gracefully: a {@code null} body summary yields {@code null} body values (no
 * fabrication) and a {@code null} training summary yields zero session counts. This covers the
 * body-only, training-only and empty-week cases without special-casing at call sites.
 */
public final class WeeklyCheckInBuilder {

  private WeeklyCheckInBuilder() {}

  /**
   * Builds a check-in for {@code weekStartDate} from the two summaries. Either summary may be
   * {@code null}; its contribution is then absent (null body values / zero training counts).
   */
  public static WeeklyCheckIn build(
      LocalDate weekStartDate,
      WeeklyBodySummary body,
      WeeklyTrainingSummary training,
      String notes) {
    return new WeeklyCheckIn(
        weekStartDate,
        body == null ? null : body.latestWeightKg(),
        body == null ? null : body.latestBodyFatPercentage(),
        body == null ? null : body.latestLeanMassKg(),
        training == null ? 0 : training.plannedRunningSessions(),
        training == null ? 0 : training.completedRunningSessions(),
        training == null ? 0 : training.plannedStrengthSessions(),
        training == null ? 0 : training.completedStrengthSessions(),
        notes);
  }
}

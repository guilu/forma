package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.domain.Streak;
import java.time.LocalDate;

/**
 * Response body for {@code GET /api/v1/progress/streak} (FOR-139 api.md).
 *
 * <p>Delivery read model, distinct from the domain {@link Streak} (ADR-005), mirroring {@code
 * AdherenceResponse}'s from-view convention.
 */
public record StreakResponse(int currentStreakDays, int longestStreakDays, LocalDate asOf) {

  public static StreakResponse from(Streak streak) {
    return new StreakResponse(
        streak.currentStreakDays(), streak.longestStreakDays(), streak.asOf());
  }
}

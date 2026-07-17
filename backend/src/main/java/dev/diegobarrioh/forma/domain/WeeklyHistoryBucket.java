package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;

/**
 * One week's planned-vs-completed bucket for the FOR-139 weekly-history bars, keyed by {@code
 * weekStart} (the Monday starting the week, mirroring {@code WeeklyCheckInService}'s week-start
 * convention).
 *
 * <p>Framework-free (ADR-001); the counting itself happens in the application layer ({@code
 * WeeklyHistoryService}), mirroring {@link CategoryAdherence}'s split between pure shape and
 * application-level counting.
 *
 * @param weekStart the Monday starting the week (inclusive)
 * @param planned the expected count for the week; never negative
 * @param completed the actual count for the week; never negative, may exceed {@code planned}
 */
public record WeeklyHistoryBucket(LocalDate weekStart, int planned, int completed) {

  public WeeklyHistoryBucket {
    if (planned < 0) {
      throw new IllegalArgumentException("planned must not be negative, was: " + planned);
    }
    if (completed < 0) {
      throw new IllegalArgumentException("completed must not be negative, was: " + completed);
    }
  }
}

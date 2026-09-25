package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Where an account's running plan sits, derived purely from two Mondays: the Monday the plan was
 * accepted, and the Monday of the week being shown. No week counter is persisted — a stored number
 * desyncs the moment somebody skips a login, and there is no job to keep it in step. The single
 * fact this is built from is the acceptance instant already stored in {@code
 * plan_acceptance.accepted_at} (or {@code cycle_started_at} after a restart).
 */
public sealed interface TrainingPlanProgress {

  /** No accepted plan to derive a week from; nothing has started yet. */
  record NotStarted() implements TrainingPlanProgress {}

  /** Week {@code weekNumber} (1..totalWeeks) of the plan, counting from the accepted Monday. */
  record Active(int weekNumber) implements TrainingPlanProgress {}

  /** Every week of the plan has already passed; the cycle does not repeat on its own. */
  record Completed() implements TrainingPlanProgress {}

  /**
   * Derives the progress from the Monday the plan was accepted and the Monday of "today", both
   * already resolved to the caller's own week-boundary {@link java.time.Clock} zone.
   *
   * @param totalWeeks the plan's length (see {@code RunningPlanGenerator#WEEKS})
   */
  static TrainingPlanProgress since(
      LocalDate acceptedWeekStart, LocalDate currentWeekStart, int totalWeeks) {
    long weeksElapsed = ChronoUnit.WEEKS.between(acceptedWeekStart, currentWeekStart);
    long weekNumber = weeksElapsed + 1;
    if (weekNumber > totalWeeks) {
      return new Completed();
    }
    return new Active((int) weekNumber);
  }
}

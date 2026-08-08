package dev.diegobarrioh.forma.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Selects the plan days that belong to the calendar week currently being shopped for. */
final class PlanWeekSelector {

  private PlanWeekSelector() {}

  /**
   * Returns only days dated from this week's Monday through Sunday.
   *
   * <p>{@link PlanDay#dateWithin(LocalDate)} is the source of truth for mapping a plan slot to a
   * calendar date. A template without a start date, or a date before/after the represented plan
   * weeks, therefore has no current week.
   */
  static List<PlanDay> currentWeek(List<PlanDay> days, LocalDate planStart, LocalDate currentDate) {
    if (planStart == null) {
      return List.of();
    }
    LocalDate monday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate sunday = monday.plusDays(6);
    return days.stream()
        .filter(
            day ->
                day.dateWithin(planStart)
                    .filter(date -> !date.isBefore(monday) && !date.isAfter(sunday))
                    .isPresent())
        .toList();
  }
}

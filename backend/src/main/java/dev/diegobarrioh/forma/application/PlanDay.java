package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * One day of a plan (V53), identified by where it falls rather than by what date it is.
 *
 * <p>{@code (weekNumber, dayNumber)} is the identity. The weekday follows from {@link #dayNumber}
 * and the calendar date follows from the plan's start date, so neither is stored — see {@link
 * #dayOfWeek()} and {@link #dateWithin(LocalDate)}. Keeping {@link #weekNumber} from the first day,
 * while every plan is a single week, is what makes a four- or twelve-week plan the same shape
 * rather than a migration.
 *
 * <p>{@link #targets} is what the day was ASKED to hit. What it adds up to is computed from its
 * meals and stored nowhere, which is the only way the two can ever disagree — and a plan that
 * cannot say "you asked for 2320 and this comes to 2100" is a plan that cannot be checked.
 *
 * @param id the row's id; null before it has been written
 * @param weekNumber which week of the plan, from 1
 * @param dayNumber which day of that week, 1 = monday
 * @param dayType RUNNING / STRENGTH / REST; null when nobody classified it
 * @param targets what this day was asked to hit; never null, possibly unset
 * @param notes free text
 * @param meals the day's meals, in order
 */
public record PlanDay(
    java.util.UUID id,
    int weekNumber,
    int dayNumber,
    NutritionDayType dayType,
    MacroTargets targets,
    String notes,
    List<PlanMeal> meals) {

  public PlanDay {
    if (weekNumber < 1) {
      throw new IllegalArgumentException("weekNumber starts at 1, was: " + weekNumber);
    }
    if (dayNumber < 1 || dayNumber > 7) {
      throw new IllegalArgumentException("dayNumber must be 1..7, was: " + dayNumber);
    }
    targets = targets == null ? MacroTargets.none() : targets;
    meals = meals == null ? List.of() : List.copyOf(meals);
  }

  /** The weekday this day falls on. Derived, never stored: {@code dayNumber} 1 is monday. */
  public DayOfWeek dayOfWeek() {
    return DayOfWeek.of(dayNumber);
  }

  /**
   * The calendar date this day lands on, given the plan's start date.
   *
   * <p>Anchored to the MONDAY of the week the plan starts in, not to the start date itself. Both
   * readings are defensible — "day 1 is the day the plan begins" or "day 1 is monday" — and they
   * disagree for any plan that starts mid-week: under the first, a plan beginning on a Wednesday
   * would have its day 1 fall on a Wednesday while {@link #dayOfWeek()} said monday. Only one of
   * the two can be true, and the weekday is the one everything else in the app already runs on
   * ({@code WeeklyTrainingDayPolicy} classifies training and nutrition days by {@link DayOfWeek}).
   *
   * <p>The cost is deliberate and small: a plan starting on a Wednesday has days 1 and 2 of its
   * first week behind it. Those days are in the past, which is a truthful thing for them to be.
   *
   * <p>Empty while the plan is a template and has no start date — which is exactly the case a
   * stored {@code calendar_date} column would have had to leave null, so storing it would have
   * added a column that is null whenever it is underivable and redundant whenever it is not.
   */
  public Optional<LocalDate> dateWithin(LocalDate planStart) {
    return Optional.ofNullable(planStart)
        .map(
            start ->
                start.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        .map(monday -> monday.plusDays(7L * (weekNumber - 1) + (dayNumber - 1)));
  }
}

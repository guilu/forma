package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;

/**
 * Resolves a calendar date to its {@link NutritionDayType} (FOR-128).
 *
 * <p>Reuses the shared {@link WeeklyTrainingDayPolicy} day classification instead of duplicating
 * it: the FOR-26 training calendar and the FOR-102/FOR-128 nutrition consumption target read the
 * exact same {@code DayOfWeek} -&gt; day-kind policy, so they can never drift apart.
 *
 * <p>Pure and deterministic (ADR-001): no persistence, no new date-to-day-type schedule. That
 * purity is also this resolver's limit — it classifies by weekday alone, so it cannot know that a
 * session was moved within the week (V60). {@code ScheduledNutritionDayTypeService} layers those
 * overrides on top and delegates here for any date outside the composed week, which keeps this the
 * single source of the underlying policy rather than a second one.
 */
public final class NutritionDayTypeResolver {

  private NutritionDayTypeResolver() {}

  /** Resolves {@code date} to its {@link NutritionDayType} via the shared weekly day policy. */
  public static NutritionDayType resolve(LocalDate date) {
    return WeeklyTrainingDayPolicy.classify(date.getDayOfWeek());
  }
}

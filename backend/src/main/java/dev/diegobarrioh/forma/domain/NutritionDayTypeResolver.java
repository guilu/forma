package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;

/**
 * Resolves a calendar date to its {@link NutritionDayType} (FOR-128).
 *
 * <p>Reuses the shared {@link WeeklyTrainingDayPolicy} day classification instead of duplicating
 * it: the FOR-26 training calendar and the FOR-102/FOR-128 nutrition consumption target read the
 * exact same {@code DayOfWeek} -&gt; day-kind policy, so they can never drift apart.
 *
 * <p>Pure and deterministic (ADR-001): no persistence, no new date-to-day-type schedule. This is
 * the documented MVP resolver (spec FOR-128 Open Questions) until a real, user-configurable
 * calendar schedule replaces it — tracked under FOR-102, explicitly out of scope here.
 */
public final class NutritionDayTypeResolver {

  private NutritionDayTypeResolver() {}

  /** Resolves {@code date} to its {@link NutritionDayType} via the shared weekly day policy. */
  public static NutritionDayType resolve(LocalDate date) {
    return WeeklyTrainingDayPolicy.classify(date.getDayOfWeek());
  }
}

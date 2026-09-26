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
 *
 * <p>The weekday classification alone is not the whole rule (FIX1): where the plan sits for that
 * date's own week — {@link TrainingPlanProgress}, derived per-week rather than only for "now" —
 * still governs it, so this takes that progress as its second parameter rather than exposing a
 * weekday-only variant callers could reach for by mistake and silently ignore plan state with.
 */
public final class NutritionDayTypeResolver {

  private NutritionDayTypeResolver() {}

  /**
   * Resolves {@code date} to its {@link NutritionDayType}, via the shared weekly day policy but
   * governed by where the plan sits for {@code date}'s own week (design D1/D4): before the plan has
   * started every day is a rest day, and past its last week running drops to rest while strength
   * keeps going — there is no weekday-only policy to fall back on for either state.
   */
  public static NutritionDayType resolve(LocalDate date, TrainingPlanProgress progress) {
    if (progress instanceof TrainingPlanProgress.NotStarted) {
      return NutritionDayType.REST;
    }
    NutritionDayType weekdayType = WeeklyTrainingDayPolicy.classify(date.getDayOfWeek());
    if (progress instanceof TrainingPlanProgress.Completed) {
      return weekdayType == NutritionDayType.RUNNING ? NutritionDayType.REST : weekdayType;
    }
    return weekdayType;
  }
}

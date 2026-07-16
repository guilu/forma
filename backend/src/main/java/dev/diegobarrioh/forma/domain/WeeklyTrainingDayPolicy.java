package dev.diegobarrioh.forma.domain;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic MVP weekly training day classification (FOR-26, extracted for FOR-128): which
 * {@link DayOfWeek} is a running day, a strength day (and with which {@link WorkoutType}), or a
 * rest day.
 *
 * <p>Single source of truth for the training day-kind policy, shared by {@code
 * dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService} (the FOR-26 training calendar)
 * and {@link NutritionDayTypeResolver} (FOR-128, the nutrition consumption target). A policy change
 * here updates both consumers instead of the two drifting apart. Framework-free (ADR-001), pure and
 * deterministic.
 *
 * <p>The MVP policy (spec FOR-26/FOR-128): running Tuesday/Thursday/Saturday (the {@link
 * RunningPlanGenerator} schedule), strength Monday (PUSH) / Wednesday (PULL) / Friday (LEGS), any
 * other day (e.g. Sunday) is rest. Running days are derived directly from {@link
 * RunningPlanGenerator#sixteenWeekPlan()} rather than a second hardcoded literal set, so the two
 * can never disagree.
 */
public final class WeeklyTrainingDayPolicy {

  private static final Set<DayOfWeek> RUNNING_DAYS =
      RunningPlanGenerator.sixteenWeekPlan().stream()
          .map(RunningPlanSession::dayOfWeek)
          .collect(() -> EnumSet.noneOf(DayOfWeek.class), Set::add, Set::addAll);

  private static final Map<DayOfWeek, WorkoutType> STRENGTH_DAYS =
      new EnumMap<>(
          Map.of(
              DayOfWeek.MONDAY, WorkoutType.PUSH,
              DayOfWeek.WEDNESDAY, WorkoutType.PULL,
              DayOfWeek.FRIDAY, WorkoutType.LEGS));

  private WeeklyTrainingDayPolicy() {}

  /** The days the running plan schedules a session on (Tuesday/Thursday/Saturday). */
  public static Set<DayOfWeek> runningDays() {
    return RUNNING_DAYS;
  }

  /** Strength day -&gt; its {@link WorkoutType} (Monday PUSH / Wednesday PULL / Friday LEGS). */
  public static Map<DayOfWeek, WorkoutType> strengthDays() {
    return STRENGTH_DAYS;
  }

  /**
   * Classifies {@code day} as a {@link NutritionDayType}: {@link NutritionDayType#RUNNING} on a
   * running day, {@link NutritionDayType#STRENGTH} on a strength day, {@link NutritionDayType#REST}
   * otherwise.
   *
   * <p>Running is checked first. Under the current MVP policy running days ({@link #runningDays()})
   * and strength days ({@link #strengthDays()}) are disjoint sets, so this ordering is not
   * reachable as a real precedence decision today; it is documented here (spec FOR-128 edge case)
   * so a future policy that allows both on the same day has a defined, intentional precedence
   * (running wins) instead of an accidental one.
   */
  public static NutritionDayType classify(DayOfWeek day) {
    if (RUNNING_DAYS.contains(day)) {
      return NutritionDayType.RUNNING;
    }
    if (STRENGTH_DAYS.containsKey(day)) {
      return NutritionDayType.STRENGTH;
    }
    return NutritionDayType.REST;
  }
}

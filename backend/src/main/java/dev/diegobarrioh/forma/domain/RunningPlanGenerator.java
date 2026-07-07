package dev.diegobarrioh.forma.domain;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the initial 16-week running progression (FOR-23) as a list of {@link
 * RunningPlanSession} (FOR-22).
 *
 * <p>Deterministic and rule-based: the same call always produces the same plan. It is a plain
 * in-code generator rather than persisted seed data — the story only requires "seed data or
 * equivalent plan generation" and this keeps the plan editable/replaceable later without a schema
 * (spec FOR-23 Open Questions). No new persisted entity or migration is introduced.
 *
 * <p>Each of the 16 weeks has exactly 3 sessions — one {@link SessionType#EASY}, one {@link
 * SessionType#INTERVALS} (quality/controlled), one {@link SessionType#LONG_RUN} — for 48 sessions
 * in total. The long run builds gradually and monotonically from ~4 km (the user's current
 * baseline) to ~10 km; paces stay conservative, expressed as target effort (RPE) rather than pace
 * (FOR-22).
 */
public final class RunningPlanGenerator {

  /** Number of weeks in the initial progression. */
  public static final int WEEKS = 16;

  /** Long-run distance at week 1 and week {@link #WEEKS}, in kilometers. */
  private static final double LONG_RUN_START_KM = 4.0;

  private static final double LONG_RUN_END_KM = 10.0;

  // Conservative RPE targets per session type (1-10 scale).
  private static final int EASY_EFFORT = 4;
  private static final int INTERVALS_EFFORT = 7;
  private static final int LONG_RUN_EFFORT = 5;

  private RunningPlanGenerator() {}

  /** Builds the full 16-week progression, ordered by week then by session day. */
  public static List<RunningPlanSession> sixteenWeekPlan() {
    List<RunningPlanSession> plan = new ArrayList<>(WEEKS * 3);
    for (int week = 1; week <= WEEKS; week++) {
      double longRunKm = longRunDistanceKm(week);
      // Easy and quality runs are shorter than the long run; conservative and moderate.
      double easyKm = roundToOneDecimal(longRunKm * 0.55);
      double intervalsKm = roundToOneDecimal(longRunKm * 0.6);

      plan.add(
          new RunningPlanSession(
              week, DayOfWeek.TUESDAY, SessionType.EASY, easyKm, EASY_EFFORT, null));
      plan.add(
          new RunningPlanSession(
              week,
              DayOfWeek.THURSDAY,
              SessionType.INTERVALS,
              intervalsKm,
              INTERVALS_EFFORT,
              null));
      plan.add(
          new RunningPlanSession(
              week, DayOfWeek.SATURDAY, SessionType.LONG_RUN, longRunKm, LONG_RUN_EFFORT, null));
    }
    return List.copyOf(plan);
  }

  /**
   * Long-run distance for a week: linear from {@link #LONG_RUN_START_KM} at week 1 to {@link
   * #LONG_RUN_END_KM} at week {@link #WEEKS}, so it grows gradually and never decreases.
   */
  private static double longRunDistanceKm(int week) {
    double step = (LONG_RUN_END_KM - LONG_RUN_START_KM) / (WEEKS - 1);
    return roundToOneDecimal(LONG_RUN_START_KM + (week - 1) * step);
  }

  private static double roundToOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}

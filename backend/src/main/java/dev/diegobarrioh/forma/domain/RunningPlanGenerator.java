package dev.diegobarrioh.forma.domain;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates the 16-week running progression (FOR-23) as a list of {@link RunningPlanSession}
 * (FOR-22), matching Diego's real plan (FOR-153, {@code docs/fitness_os.xlsm} sheet "Running").
 *
 * <p>Deterministic and rule-based: the same call always produces the same plan. It is a plain
 * in-code generator rather than persisted seed data — the story only requires "seed data or
 * equivalent plan generation" and this keeps the plan editable/replaceable later without a schema
 * (spec FOR-23 Open Questions, reconfirmed by FOR-153 which needs no migration).
 *
 * <p>Each of the 16 weeks has exactly 3 sessions: one {@link SessionType#EASY} ("suave"), one
 * quality session on Wednesday, and one {@link SessionType#LONG_RUN} ("largo") — for 48 sessions in
 * total. Sessions are scheduled on Monday/Wednesday/Saturday (FOR-151: Diego's real plan). This is
 * the single source of truth for the running days — {@link WeeklyTrainingDayPolicy#runningDays()}
 * derives directly from this method's output rather than duplicating a second literal day set.
 * FOR-153 only changes volume/structure below; it does not touch which day each session falls on.
 *
 * <p><b>Volume curve (FOR-153).</b> The long run builds from {@value #LONG_RUN_START_KM} km (week
 * 1) to {@value #LONG_RUN_END_KM} km, reaching its ceiling by week {@value #LONG_RUN_RAMP_WEEKS}
 * and then plateauing for the remaining weeks (Excel edge case: "weeks 14-16 plateau at 10 km long
 * run, no further increase"). The easy run steps from 4 km (weeks 1-4) to 5 km (weeks 5-16). The
 * Wednesday session is a fixed {@value #INTERVALS_KM} km 6&times;400m interval structure (reps plus
 * warm-up/cool-down/recovery jogging — the STRUCTURE is fixed per the Excel, not a distance derived
 * from the long run); its distance is represented via {@link RunningPlanSession#notes()} rather
 * than a new structured field (FOR-153 Data Model Notes: prefer the minimal representation).
 *
 * <p><b>Deload weeks ({@value #DELOAD_WEEK_1}/{@value #DELOAD_WEEK_2}/{@value
 * #DELOAD_WEEK_3}/{@value #DELOAD_WEEK_4}).</b> The Wednesday session becomes a short
 * easy/"descarga" {@link SessionType#RECOVERY} run instead of 6&times;400m, dropping that week's
 * total distance below the surrounding trend — the weekly-volume curve is intentionally
 * non-monotone at these weeks. The long run and easy run are unaffected by deload weeks (the long
 * run's own ramp already plateaus by week {@value #LONG_RUN_RAMP_WEEKS}, so this cannot conflict
 * with the "no further increase" plateau edge case).
 *
 * <p><b>Documented reconciliation (FOR-153 spec Data Model Notes / Open Questions).</b> The Excel's
 * "Km semana" column (13.0...19.0, one value per week) is a stated *weekly target*; it does not
 * equal the literal sum of the three per-session distances once the fixed 6&times;400m structure
 * and the deload dips are modeled explicitly (the spec explicitly flags this as a design decision
 * to reconcile). This generator emits the *derived* total (the literal sum of the three sessions)
 * as the actual weekly volume, matching how {@code WeeklyTrainingSummaryService} already computes
 * planned distance from individual sessions. With {@link #INTERVALS_KM} = 4.0 km (a realistic total
 * for 6&times;400m including warm-up/cool-down/recovery jogging, not just the 2.4 km of raw reps),
 * the derived curve matches the Excel's stated 13&rarr;19 km/week range closely (exact at week 1 =
 * 13.0 km and weeks 14-15 = 19.0 km; within 0.1-0.2 km elsewhere), while the four deload weeks —
 * which the literal Excel "Km semana" row does not show as a numeric dip — are modeled as a real
 * reduction versus the prior week, per the story's explicit "non-monotone at deload weeks"
 * requirement (spec.md Edge Cases, tests.md Domain Tests).
 */
public final class RunningPlanGenerator {

  /** Number of weeks in the progression. */
  public static final int WEEKS = 16;

  /** Long-run distance at week 1, in kilometers. */
  private static final double LONG_RUN_START_KM = 5.0;

  /** Long-run distance once it reaches its ceiling (week {@link #LONG_RUN_RAMP_WEEKS} onward). */
  private static final double LONG_RUN_END_KM = 10.0;

  /** Week the long run first reaches {@link #LONG_RUN_END_KM}; it plateaus from here on. */
  private static final int LONG_RUN_RAMP_WEEKS = 13;

  /** Easy-run distance for weeks 1-4. */
  private static final double EASY_KM_EARLY = 4.0;

  /** Easy-run distance for weeks 5-16. */
  private static final double EASY_KM_LATER = 5.0;

  /** First week the easy run steps up to {@link #EASY_KM_LATER}. */
  private static final int EASY_KM_TRANSITION_WEEK = 5;

  /** Fixed distance for the 6x400m interval session on non-deload weeks. */
  private static final double INTERVALS_KM = 4.0;

  /** Distance for the easy/"descarga" recovery run that replaces intervals on deload weeks. */
  private static final double DELOAD_RECOVERY_KM = 3.0;

  // Conservative RPE targets per session type (1-10 scale). Session 1 (easy) and the long run per
  // the Excel ("Ritmo 5:45-6:15/km, RPE 6-7" for easy; "conversacional" for the long run).
  private static final int EASY_EFFORT = 6;
  private static final int INTERVALS_EFFORT = 7;
  private static final int LONG_RUN_EFFORT = 5;
  private static final int DELOAD_EFFORT = 3;

  private static final int DELOAD_WEEK_1 = 4;
  private static final int DELOAD_WEEK_2 = 8;
  private static final int DELOAD_WEEK_3 = 12;
  private static final int DELOAD_WEEK_4 = 16;

  private static final Set<Integer> DELOAD_WEEKS =
      Set.of(DELOAD_WEEK_1, DELOAD_WEEK_2, DELOAD_WEEK_3, DELOAD_WEEK_4);

  private static final String INTERVALS_NOTES =
      "6x400m con trote de recuperación suave entre series (calentamiento + enfriamiento incluidos)";

  private static final String DELOAD_NOTES =
      "Rodaje muy suave / descarga (semana de descarga cada 4 semanas)";

  private RunningPlanGenerator() {}

  /** Builds the full 16-week progression, ordered by week then by session day. */
  public static List<RunningPlanSession> sixteenWeekPlan() {
    List<RunningPlanSession> plan = new ArrayList<>(WEEKS * 3);
    for (int week = 1; week <= WEEKS; week++) {
      double easyKm = week < EASY_KM_TRANSITION_WEEK ? EASY_KM_EARLY : EASY_KM_LATER;
      double longRunKm = longRunDistanceKm(week);
      boolean deload = DELOAD_WEEKS.contains(week);

      plan.add(
          new RunningPlanSession(
              week, DayOfWeek.MONDAY, SessionType.EASY, easyKm, EASY_EFFORT, null));
      plan.add(wednesdaySession(week, deload));
      plan.add(
          new RunningPlanSession(
              week, DayOfWeek.SATURDAY, SessionType.LONG_RUN, longRunKm, LONG_RUN_EFFORT, null));
    }
    return List.copyOf(plan);
  }

  private static RunningPlanSession wednesdaySession(int week, boolean deload) {
    if (deload) {
      return new RunningPlanSession(
          week,
          DayOfWeek.WEDNESDAY,
          SessionType.RECOVERY,
          DELOAD_RECOVERY_KM,
          DELOAD_EFFORT,
          DELOAD_NOTES);
    }
    return new RunningPlanSession(
        week,
        DayOfWeek.WEDNESDAY,
        SessionType.INTERVALS,
        INTERVALS_KM,
        INTERVALS_EFFORT,
        INTERVALS_NOTES);
  }

  /**
   * Long-run distance for a week: linear from {@link #LONG_RUN_START_KM} at week 1 to {@link
   * #LONG_RUN_END_KM} at week {@link #LONG_RUN_RAMP_WEEKS}, then flat at {@link #LONG_RUN_END_KM}
   * for the remaining weeks (deload weeks do not affect the long run — see class Javadoc).
   */
  private static double longRunDistanceKm(int week) {
    if (week >= LONG_RUN_RAMP_WEEKS) {
      return LONG_RUN_END_KM;
    }
    double step = (LONG_RUN_END_KM - LONG_RUN_START_KM) / (LONG_RUN_RAMP_WEEKS - 1);
    return roundToOneDecimal(LONG_RUN_START_KM + (week - 1) * step);
  }

  private static double roundToOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}

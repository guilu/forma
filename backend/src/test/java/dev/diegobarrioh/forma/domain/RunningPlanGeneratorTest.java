package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the 16-week running progression (FOR-23, revised by FOR-153 to Diego's real
 * plan: {@code docs/fitness_os.xlsm} sheet "Running"). Plain JUnit 5 + AssertJ (ADR-007). Asserts
 * the plan's structure, the 13&rarr;19 km/week volume curve, the fixed 6&times;400m session 2 and
 * the deload weeks (4/8/12/16).
 */
class RunningPlanGeneratorTest {

  private static final Set<Integer> DELOAD_WEEKS = Set.of(4, 8, 12, 16);

  private final List<RunningPlanSession> plan = RunningPlanGenerator.sixteenWeekPlan();

  @Test
  @DisplayName("has 16 weeks with 3 sessions each (48 total)")
  void hasSixteenWeeksOfThreeSessions() {
    assertThat(plan).hasSize(48);

    Map<Integer, List<RunningPlanSession>> byWeek =
        plan.stream().collect(Collectors.groupingBy(RunningPlanSession::weekNumber));
    assertThat(byWeek.keySet()).hasSize(16);
    assertThat(byWeek.values()).allSatisfy(week -> assertThat(week).hasSize(3));
  }

  @Test
  @DisplayName("sessions stay scheduled on Monday/Wednesday/Saturday (FOR-151, unaffected)")
  void sessionsStayOnTheRealPlanDays() {
    assertThat(plan)
        .extracting(RunningPlanSession::dayOfWeek)
        .containsOnly(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.SATURDAY);
  }

  @Test
  @DisplayName("non-deload weeks have one easy, one intervals and one long run")
  void nonDeloadWeeksHaveEasyIntervalsAndLongRun() {
    Map<Integer, Set<SessionType>> typesByWeek = typesByWeek();

    typesByWeek.forEach(
        (week, types) -> {
          if (!DELOAD_WEEKS.contains(week)) {
            assertThat(types)
                .as("week %d session types", week)
                .containsExactlyInAnyOrder(
                    SessionType.EASY, SessionType.INTERVALS, SessionType.LONG_RUN);
          }
        });
  }

  @Test
  @DisplayName("deload weeks (4/8/12/16) replace intervals with an easy/descarga recovery run")
  void deloadWeeksHaveEasyRecoveryAndLongRun() {
    Map<Integer, Set<SessionType>> typesByWeek = typesByWeek();

    for (int week : DELOAD_WEEKS) {
      assertThat(typesByWeek.get(week))
          .as("week %d session types", week)
          .containsExactlyInAnyOrder(SessionType.EASY, SessionType.RECOVERY, SessionType.LONG_RUN);
    }
  }

  @Test
  @DisplayName("long run builds from 5.0 km to 10.0 km and plateaus weeks 13-16")
  void longRunBuildsFromFiveToTenKmAndPlateaus() {
    Map<Integer, Double> longRunByWeek = distancesByWeek(SessionType.LONG_RUN);

    assertThat(longRunByWeek.get(1)).isEqualTo(5.0);
    assertThat(longRunByWeek.get(13)).isEqualTo(10.0);
    assertThat(longRunByWeek.get(14)).isEqualTo(10.0);
    assertThat(longRunByWeek.get(15)).isEqualTo(10.0);
    assertThat(longRunByWeek.get(16)).isEqualTo(10.0);

    // Never decreases and never exceeds the 10.0 km ceiling.
    List<Double> ordered =
        longRunByWeek.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .toList();
    for (int i = 1; i < ordered.size(); i++) {
      assertThat(ordered.get(i)).isGreaterThanOrEqualTo(ordered.get(i - 1));
      assertThat(ordered.get(i)).isLessThanOrEqualTo(10.0);
    }
  }

  @Test
  @DisplayName("session 1 (easy) is 4 km weeks 1-4 then 5 km weeks 5-16")
  void sessionOneStepsFromFourToFiveKm() {
    Map<Integer, Double> easyByWeek = distancesByWeek(SessionType.EASY);

    for (int week = 1; week <= 4; week++) {
      assertThat(easyByWeek.get(week)).as("week %d easy km", week).isEqualTo(4.0);
    }
    for (int week = 5; week <= 16; week++) {
      assertThat(easyByWeek.get(week)).as("week %d easy km", week).isEqualTo(5.0);
    }
  }

  @Test
  @DisplayName("session 2 is a fixed 6x400m interval structure on non-deload weeks")
  void intervalsSessionIsAFixedSixByFourHundredStructure() {
    List<RunningPlanSession> intervalSessions =
        plan.stream().filter(s -> s.sessionType() == SessionType.INTERVALS).toList();

    assertThat(intervalSessions).hasSize(12); // 16 weeks - 4 deload weeks
    assertThat(intervalSessions)
        .allSatisfy(
            session -> {
              assertThat(session.targetDistanceKm()).isGreaterThan(0);
              assertThat(session.notes()).isNotNull();
              assertThat(session.notes().toLowerCase(java.util.Locale.ROOT))
                  .contains("400m")
                  .contains("6");
            });
    // Fixed structure -> same distance every non-deload week.
    assertThat(intervalSessions.stream().map(RunningPlanSession::targetDistanceKm).distinct())
        .hasSize(1);
  }

  @Test
  @DisplayName("deload weeks describe the session as easy/descarga, not 6x400m")
  void deloadRecoverySessionIsDescribedAsDescarga() {
    List<RunningPlanSession> deloadSessions =
        plan.stream()
            .filter(s -> DELOAD_WEEKS.contains(s.weekNumber()))
            .filter(s -> s.sessionType() == SessionType.RECOVERY)
            .toList();

    assertThat(deloadSessions).hasSize(4);
    assertThat(deloadSessions)
        .allSatisfy(
            session -> {
              assertThat(session.targetDistanceKm()).isGreaterThan(0);
              assertThat(session.notes()).isNotNull();
              assertThat(session.notes().toLowerCase(java.util.Locale.ROOT)).contains("descarga");
            });
  }

  @Test
  @DisplayName("weekly volume follows the Excel curve: starts at 13 km, plateaus at 19 km")
  void weeklyVolumeFollowsTheExcelCurve() {
    Map<Integer, Double> volumeByWeek = weeklyVolumeByWeek();

    assertThat(volumeByWeek.get(1)).isCloseTo(13.0, within(0.05));
    assertThat(volumeByWeek.get(14)).isCloseTo(19.0, within(0.05));
    assertThat(volumeByWeek.get(15)).isCloseTo(19.0, within(0.05));

    // Every week stays within the Excel's stated 13-19 km/week range.
    assertThat(volumeByWeek.values()).allSatisfy(km -> assertThat(km).isBetween(13.0, 19.0));
  }

  @Test
  @DisplayName("deload weeks (4/8/12/16) dip the weekly volume vs the prior week's trend")
  void deloadWeeksDipTheWeeklyVolume() {
    Map<Integer, Double> volumeByWeek = weeklyVolumeByWeek();

    for (int week : DELOAD_WEEKS) {
      double thisWeek = volumeByWeek.get(week);
      double priorWeek = volumeByWeek.get(week - 1);
      assertThat(thisWeek)
          .as(
              "week %d volume (%.1f) should dip below week %d (%.1f)",
              week, thisWeek, week - 1, priorWeek)
          .isLessThan(priorWeek);
    }
  }

  @Test
  @DisplayName("generation is deterministic")
  void isDeterministic() {
    assertThat(RunningPlanGenerator.sixteenWeekPlan())
        .isEqualTo(RunningPlanGenerator.sixteenWeekPlan());
  }

  private Map<Integer, Set<SessionType>> typesByWeek() {
    return plan.stream()
        .collect(
            Collectors.groupingBy(
                RunningPlanSession::weekNumber,
                Collectors.mapping(RunningPlanSession::sessionType, Collectors.toSet())));
  }

  private Map<Integer, Double> distancesByWeek(SessionType type) {
    return plan.stream()
        .filter(s -> s.sessionType() == type)
        .collect(
            Collectors.toMap(RunningPlanSession::weekNumber, RunningPlanSession::targetDistanceKm));
  }

  private Map<Integer, Double> weeklyVolumeByWeek() {
    return plan.stream()
        .collect(
            Collectors.groupingBy(
                RunningPlanSession::weekNumber,
                Collectors.summingDouble(RunningPlanSession::targetDistanceKm)));
  }
}

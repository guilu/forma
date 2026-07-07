package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the 16-week running progression (FOR-23). Plain JUnit 5 + AssertJ
 * (ADR-007). Asserts the plan's structure and the gradual long-run build.
 */
class RunningPlanGeneratorTest {

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
  @DisplayName("each week has one easy, one intervals and one long run")
  void eachWeekHasTheThreeSessionTypes() {
    Map<Integer, Set<SessionType>> typesByWeek =
        plan.stream()
            .collect(
                Collectors.groupingBy(
                    RunningPlanSession::weekNumber,
                    Collectors.mapping(RunningPlanSession::sessionType, Collectors.toSet())));

    assertThat(typesByWeek.values())
        .allSatisfy(
            types ->
                assertThat(types)
                    .containsExactlyInAnyOrder(
                        SessionType.EASY, SessionType.INTERVALS, SessionType.LONG_RUN));
  }

  @Test
  @DisplayName("long run builds gradually and monotonically from ~4 km to ~10 km")
  void longRunProgressesGradually() {
    List<Double> longRuns =
        plan.stream()
            .filter(s -> s.sessionType() == SessionType.LONG_RUN)
            .sorted((a, b) -> Integer.compare(a.weekNumber(), b.weekNumber()))
            .map(RunningPlanSession::targetDistanceKm)
            .toList();

    assertThat(longRuns).hasSize(16);
    assertThat(longRuns.get(0)).isEqualTo(4.0);
    assertThat(longRuns.get(15)).isEqualTo(10.0);
    // Monotonically non-decreasing week over week.
    for (int i = 1; i < longRuns.size(); i++) {
      assertThat(longRuns.get(i)).isGreaterThanOrEqualTo(longRuns.get(i - 1));
    }
  }

  @Test
  @DisplayName("generation is deterministic")
  void isDeterministic() {
    assertThat(RunningPlanGenerator.sixteenWeekPlan())
        .isEqualTo(RunningPlanGenerator.sixteenWeekPlan());
  }
}

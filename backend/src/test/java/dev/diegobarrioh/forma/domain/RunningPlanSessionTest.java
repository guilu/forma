package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link RunningPlanSession} (FOR-22). Plain JUnit 5 + AssertJ, no Spring
 * (ADR-007). Covers creation, the constrained session type, multi-week support, and construction
 * validation.
 */
class RunningPlanSessionTest {

  @Test
  @DisplayName("creates a valid planned session with all fields")
  void createsValidSession() {
    RunningPlanSession session =
        new RunningPlanSession(1, DayOfWeek.MONDAY, SessionType.EASY, 4.0, 4, "Ritmo cómodo");

    assertThat(session.weekNumber()).isEqualTo(1);
    assertThat(session.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(session.sessionType()).isEqualTo(SessionType.EASY);
    assertThat(session.targetDistanceKm()).isEqualTo(4.0);
    assertThat(session.targetEffort()).isEqualTo(4);
  }

  @Test
  @DisplayName("session type is constrained to the known values")
  void sessionTypeIsConstrained() {
    // The enum itself is the constraint: only the four known values exist.
    assertThat(SessionType.values())
        .containsExactlyInAnyOrder(
            SessionType.EASY, SessionType.LONG_RUN, SessionType.INTERVALS, SessionType.RECOVERY);
  }

  @Test
  @DisplayName("supports sessions across multiple weeks")
  void supportsMultiWeekPlan() {
    List<RunningPlanSession> plan =
        List.of(
            new RunningPlanSession(1, DayOfWeek.SATURDAY, SessionType.LONG_RUN, 4.0, 5, null),
            new RunningPlanSession(8, DayOfWeek.SATURDAY, SessionType.LONG_RUN, 7.0, 6, null),
            new RunningPlanSession(16, DayOfWeek.SATURDAY, SessionType.LONG_RUN, 10.0, 6, null));

    assertThat(plan).extracting(RunningPlanSession::weekNumber).containsExactly(1, 8, 16);
    assertThat(plan)
        .extracting(RunningPlanSession::targetDistanceKm)
        .containsExactly(4.0, 7.0, 10.0);
  }

  @Nested
  @DisplayName("construction validation")
  class Validation {

    @Test
    @DisplayName("rejects a week number below 1")
    void rejectsWeekBelowOne() {
      assertThatThrownBy(
              () -> new RunningPlanSession(0, DayOfWeek.MONDAY, SessionType.EASY, 4.0, 4, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("weekNumber");
    }

    @Test
    @DisplayName("rejects a non-positive target distance")
    void rejectsNonPositiveDistance() {
      assertThatThrownBy(
              () -> new RunningPlanSession(1, DayOfWeek.MONDAY, SessionType.EASY, 0.0, 4, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("targetDistanceKm");
    }

    @Test
    @DisplayName("rejects a target effort outside the RPE range")
    void rejectsEffortOutOfRange() {
      assertThatThrownBy(
              () -> new RunningPlanSession(1, DayOfWeek.MONDAY, SessionType.EASY, 4.0, 11, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("targetEffort");
    }

    @Test
    @DisplayName("requires dayOfWeek and sessionType")
    void requiresEnums() {
      assertThatThrownBy(() -> new RunningPlanSession(1, null, SessionType.EASY, 4.0, 4, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("dayOfWeek");
      assertThatThrownBy(() -> new RunningPlanSession(1, DayOfWeek.MONDAY, null, 4.0, 4, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("sessionType");
    }
  }
}

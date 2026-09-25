package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingSummaryService} (FOR-28; week progression by the
 * training-progression-and-logging change, design D4): planned/completed counts, running distances,
 * the empty state, and the terminal-state message once the plan's 16 weeks are behind the account.
 * Uses the real FOR-23/FOR-25 services with in-memory repositories (ADR-007).
 */
class WeeklyTrainingSummaryServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026, the account's own acceptance Monday. */
  private static final Instant ACCEPTED_MONDAY = Instant.parse("2026-08-17T09:00:00Z");

  private final FakeTrainingSessionStatusRepository statusRepository =
      new FakeTrainingSessionStatusRepository();
  private final FakePlanAcceptanceRepository acceptanceRepository =
      new FakePlanAcceptanceRepository();
  private final RunningPlanService runningPlanService = new RunningPlanService();

  private WeeklyTrainingSummaryService serviceAt(Instant now) {
    acceptanceRepository.markAccepted(USER_ID, ACCEPTED_MONDAY);
    WeeklyTrainingScheduleService scheduleService =
        new WeeklyTrainingScheduleService(
            runningPlanService,
            new WorkoutTemplateService(),
            statusRepository,
            () -> USER_ID,
            Clock.fixed(now, ZoneOffset.UTC),
            acceptanceRepository);
    return new WeeklyTrainingSummaryService(scheduleService, runningPlanService);
  }

  private final WeeklyTrainingSummaryService service = serviceAt(ACCEPTED_MONDAY);

  /** Marks a session done in whatever week the schedule service currently considers current. */
  private void complete(String sessionKey) {
    statusRepository.upsertStatus(
        USER_ID,
        ACCEPTED_MONDAY.atZone(ZoneOffset.UTC).toLocalDate(),
        sessionKey,
        SessionStatus.COMPLETED,
        null,
        null);
  }

  @Test
  void countsPlannedSessionsAndSumsPlannedDistanceWhenNothingCompleted() {
    WeeklyTrainingSummary summary = service.currentSummary();

    assertThat(summary.plannedRunningSessions()).isEqualTo(3);
    assertThat(summary.plannedStrengthSessions()).isEqualTo(3);
    assertThat(summary.completedRunningSessions()).isZero();
    assertThat(summary.completedStrengthSessions()).isZero();
    // Week 1 (FOR-153 real plan): easy 4.0 + intervals (6x400m) 4.0 + long 5.0 = 13.0 km.
    assertThat(summary.totalPlannedRunningKm()).isCloseTo(13.0, within(1e-9));
    assertThat(summary.completedRunningKm()).isZero();
    assertThat(summary.message()).contains("Carrera: 0/3 sesiones (0.0/13.0 km)");
  }

  @Test
  void countsCompletedRunningAndItsDistance() {
    complete("RUNNING:LONG_RUN");

    WeeklyTrainingSummary summary = service.currentSummary();

    assertThat(summary.completedRunningSessions()).isEqualTo(1);
    // The Saturday long run (week 1, FOR-153 real plan) is 5.0 km.
    assertThat(summary.completedRunningKm()).isCloseTo(5.0, within(1e-9));
  }

  @Test
  void countsCompletedStrength() {
    complete("STRENGTH:PUSH");

    assertThat(service.currentSummary().completedStrengthSessions()).isEqualTo(1);
  }

  @Test
  void reportsEmptyWeekWhenNothingIsPlanned() {
    WeeklyTrainingScheduleService emptySchedule = mock(WeeklyTrainingScheduleService.class);
    when(emptySchedule.currentWeek())
        .thenReturn(new WeeklyTrainingSchedule(List.of(), "NOT_STARTED", null, 16));
    RunningPlanService emptyPlan = mock(RunningPlanService.class);
    when(emptyPlan.currentPlan()).thenReturn(List.of());

    WeeklyTrainingSummary summary =
        new WeeklyTrainingSummaryService(emptySchedule, emptyPlan).currentSummary();

    assertThat(summary.plannedRunningSessions()).isZero();
    assertThat(summary.plannedStrengthSessions()).isZero();
    assertThat(summary.message()).isEqualTo("No hay entrenamientos planificados esta semana.");
  }

  /** Week 13 (FOR-153 real plan): the long run grows to 10.0 km. */
  @Test
  void weekThirteenPlansTheTenKilometerLongRun() {
    Instant twelveWeeksLater = ACCEPTED_MONDAY.plus(12 * 7, ChronoUnit.DAYS);

    WeeklyTrainingSummary summary = serviceAt(twelveWeeksLater).currentSummary();

    // Week 13: easy 5.0 + intervals 4.0 + long run (ceiling reached) 10.0 = 19.0 km.
    assertThat(summary.totalPlannedRunningKm()).isCloseTo(19.0, within(1e-9));
  }

  /**
   * D4: past the plan's 16 weeks, running reports 0/0 (its sessions are gone from the schedule)
   * while strength — which carries no plan-week concept — keeps reporting the real count.
   */
  @Test
  void completedPlanReportsNoRunningButRealStrengthCounts() {
    Instant sixteenWeeksLater = ACCEPTED_MONDAY.plus(16 * 7, ChronoUnit.DAYS);

    WeeklyTrainingSummary summary = serviceAt(sixteenWeeksLater).currentSummary();

    assertThat(summary.plannedRunningSessions()).isZero();
    assertThat(summary.completedRunningSessions()).isZero();
    assertThat(summary.totalPlannedRunningKm()).isZero();
    assertThat(summary.completedRunningKm()).isZero();
    assertThat(summary.plannedStrengthSessions()).isEqualTo(3);
    assertThat(summary.message()).contains("Carrera: 0/0 sesiones (0.0/0.0 km)");
  }
}

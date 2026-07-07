package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingSummaryService} (FOR-28): planned/completed counts, running
 * distances, and the empty state. Uses the real FOR-23/FOR-25 services with an in-memory status
 * repository (ADR-007).
 */
class WeeklyTrainingSummaryServiceTest {

  private final FakeStatusRepository statusRepository = new FakeStatusRepository();
  private final RunningPlanService runningPlanService = new RunningPlanService();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          runningPlanService, new WorkoutTemplateService(), statusRepository);
  private final WeeklyTrainingSummaryService service =
      new WeeklyTrainingSummaryService(scheduleService, runningPlanService);

  @Test
  void countsPlannedSessionsAndSumsPlannedDistanceWhenNothingCompleted() {
    WeeklyTrainingSummary summary = service.currentSummary();

    assertThat(summary.plannedRunningSessions()).isEqualTo(3);
    assertThat(summary.plannedStrengthSessions()).isEqualTo(3);
    assertThat(summary.completedRunningSessions()).isZero();
    assertThat(summary.completedStrengthSessions()).isZero();
    // Week 1: easy 2.2 + intervals 2.4 + long 4.0 = 8.6 km.
    assertThat(summary.totalPlannedRunningKm()).isCloseTo(8.6, within(1e-9));
    assertThat(summary.completedRunningKm()).isZero();
    assertThat(summary.message()).contains("Carrera: 0/3 sesiones (0.0/8.6 km)");
  }

  @Test
  void countsCompletedRunningAndItsDistance() {
    statusRepository.upsert("SATURDAY:RUNNING", SessionStatus.COMPLETED, null);

    WeeklyTrainingSummary summary = service.currentSummary();

    assertThat(summary.completedRunningSessions()).isEqualTo(1);
    // The Saturday long run is 4.0 km.
    assertThat(summary.completedRunningKm()).isCloseTo(4.0, within(1e-9));
  }

  @Test
  void countsCompletedStrength() {
    statusRepository.upsert("MONDAY:STRENGTH", SessionStatus.COMPLETED, null);

    assertThat(service.currentSummary().completedStrengthSessions()).isEqualTo(1);
  }

  @Test
  void reportsEmptyWeekWhenNothingIsPlanned() {
    WeeklyTrainingScheduleService emptySchedule = mock(WeeklyTrainingScheduleService.class);
    when(emptySchedule.currentWeek()).thenReturn(new WeeklyTrainingSchedule(List.of()));
    RunningPlanService emptyPlan = mock(RunningPlanService.class);
    when(emptyPlan.currentPlan()).thenReturn(List.of());

    WeeklyTrainingSummary summary =
        new WeeklyTrainingSummaryService(emptySchedule, emptyPlan).currentSummary();

    assertThat(summary.plannedRunningSessions()).isZero();
    assertThat(summary.plannedStrengthSessions()).isZero();
    assertThat(summary.message()).isEqualTo("No hay entrenamientos planificados esta semana.");
  }

  /** In-memory {@link TrainingSessionStatusRepository} for unit tests. */
  static final class FakeStatusRepository implements TrainingSessionStatusRepository {
    private final Map<String, StoredSessionStatus> stored = new HashMap<>();

    @Override
    public Map<String, StoredSessionStatus> findAll() {
      return stored;
    }

    @Override
    public void upsert(String sessionId, SessionStatus status, String notes) {
      stored.put(sessionId, new StoredSessionStatus(sessionId, status, notes));
    }
  }
}

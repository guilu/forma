package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingScheduleService} (FOR-26/FOR-27): composes running + strength
 * + rest days from the real FOR-23/FOR-25 services and applies stored status (no Spring — ADR-007).
 */
class WeeklyTrainingScheduleServiceTest {

  private final FakeStatusRepository statusRepository = new FakeStatusRepository();
  private final WeeklyTrainingScheduleService service =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), statusRepository);

  private Map<DayOfWeek, TrainingDay> byDay() {
    return service.currentWeek().days().stream()
        .collect(Collectors.toMap(TrainingDay::dayOfWeek, Function.identity()));
  }

  @Test
  void hasSevenDaysMondayThroughSunday() {
    assertThat(service.currentWeek().days())
        .extracting(TrainingDay::dayOfWeek)
        .containsExactly(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY);
  }

  @Test
  void placesRunningAndStrengthWithStableIds() {
    Map<DayOfWeek, TrainingDay> days = byDay();

    assertThat(days.get(DayOfWeek.SATURDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("RUNNING");
              assertThat(entry.id()).isEqualTo("SATURDAY:RUNNING");
              assertThat(entry.title()).isEqualTo("Tirada larga");
              // Week 1 long run under the FOR-153 real plan is 5.0 km.
              assertThat(entry.detail()).isEqualTo("5.0 km");
            });
    assertThat(days.get(DayOfWeek.TUESDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("STRENGTH");
              assertThat(entry.id()).isEqualTo("TUESDAY:STRENGTH");
            });
  }

  @Test
  void fridayIsARestDay() {
    assertThat(byDay().get(DayOfWeek.FRIDAY).isRest()).isTrue();
  }

  @Test
  void defaultsToPlannedWhenNoStoredStatus() {
    assertThat(service.currentWeek().days())
        .allSatisfy(
            day ->
                assertThat(day.entries())
                    .allSatisfy(entry -> assertThat(entry.status()).isEqualTo("PLANNED")));
  }

  @Test
  void appliesStoredStatusAndNotes() {
    statusRepository.upsert("SATURDAY:RUNNING", SessionStatus.COMPLETED, "Buenas sensaciones");

    TrainingEntry saturdayRun =
        byDay().get(DayOfWeek.SATURDAY).entries().stream()
            .filter(entry -> entry.id().equals("SATURDAY:RUNNING"))
            .findFirst()
            .orElseThrow();

    assertThat(saturdayRun.status()).isEqualTo("COMPLETED");
    assertThat(saturdayRun.notes()).isEqualTo("Buenas sensaciones");
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

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingScheduleService} (FOR-26): composes running + strength + rest
 * days from the real FOR-23/FOR-25 services (no Spring context — ADR-007).
 */
class WeeklyTrainingScheduleServiceTest {

  private final WeeklyTrainingScheduleService service =
      new WeeklyTrainingScheduleService(new RunningPlanService(), new WorkoutTemplateService());

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
  void placesRunningSessionsOnTheirDays() {
    Map<DayOfWeek, TrainingDay> days = byDay();

    assertThat(days.get(DayOfWeek.TUESDAY).entries())
        .extracting(TrainingEntry::kind)
        .contains("RUNNING");
    assertThat(days.get(DayOfWeek.THURSDAY).entries())
        .extracting(TrainingEntry::kind)
        .contains("RUNNING");
    // Week 1 long run is 4.0 km (FOR-23 generator baseline).
    assertThat(days.get(DayOfWeek.SATURDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("RUNNING");
              assertThat(entry.title()).isEqualTo("Tirada larga");
              assertThat(entry.detail()).isEqualTo("4.0 km");
            });
  }

  @Test
  void placesStrengthTemplatesOnAssignedDays() {
    Map<DayOfWeek, TrainingDay> days = byDay();

    assertThat(days.get(DayOfWeek.MONDAY).entries())
        .extracting(TrainingEntry::kind)
        .contains("STRENGTH");
    assertThat(days.get(DayOfWeek.WEDNESDAY).entries())
        .extracting(TrainingEntry::kind)
        .contains("STRENGTH");
    assertThat(days.get(DayOfWeek.FRIDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("STRENGTH");
              assertThat(entry.title()).isEqualTo("Fuerza · Pierna y core");
            });
  }

  @Test
  void sundayIsARestDay() {
    assertThat(byDay().get(DayOfWeek.SUNDAY).isRest()).isTrue();
  }

  @Test
  void allEntriesArePlanned() {
    assertThat(service.currentWeek().days())
        .allSatisfy(
            day ->
                assertThat(day.entries())
                    .allSatisfy(entry -> assertThat(entry.status()).isEqualTo("PLANNED")));
  }
}

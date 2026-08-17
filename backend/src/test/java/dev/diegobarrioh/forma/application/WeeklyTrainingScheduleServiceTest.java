package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.BodyView;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingScheduleService} (FOR-26/FOR-27, re-keyed by V60): composes
 * running + strength + rest days from the real FOR-23/FOR-25 services, applies this week's stored
 * status, and honours a stored day override (no Spring — ADR-007).
 */
class WeeklyTrainingScheduleServiceTest {

  static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026, so "this week" is a fixed, readable date in every assertion. */
  private static final Instant MONDAY = Instant.parse("2026-08-17T09:00:00Z");

  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);
  private static final LocalDate LAST_WEEK = LocalDate.of(2026, 8, 10);

  private final FakeTrainingSessionStatusRepository statusRepository =
      new FakeTrainingSessionStatusRepository();
  private final WeeklyTrainingScheduleService service = serviceAt(MONDAY);

  private WeeklyTrainingScheduleService serviceAt(Instant now) {
    return new WeeklyTrainingScheduleService(
        new RunningPlanService(),
        new WorkoutTemplateService(),
        statusRepository,
        () -> USER_ID,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private Map<DayOfWeek, TrainingDay> byDay() {
    return service.currentWeek().days().stream()
        .collect(Collectors.toMap(TrainingDay::dayOfWeek, Function.identity()));
  }

  private TrainingEntry entry(DayOfWeek day, String id) {
    return byDay().get(day).entries().stream()
        .filter(candidate -> candidate.id().equals(id))
        .findFirst()
        .orElseThrow();
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
  void namesSessionsByWhatTheyAreRatherThanTheDayTheyFallOn() {
    Map<DayOfWeek, TrainingDay> days = byDay();

    assertThat(days.get(DayOfWeek.SATURDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("RUNNING");
              assertThat(entry.workoutType()).isNull();
              assertThat(entry.bodyView()).isEqualTo(BodyView.FRONT);
              // No day in the id: that is what lets the session move (V60).
              assertThat(entry.id()).isEqualTo("RUNNING:LONG_RUN");
              assertThat(entry.title()).isEqualTo("Tirada larga");
              // Week 1 long run under the FOR-153 real plan is 5.0 km.
              assertThat(entry.detail()).isEqualTo("5.0 km");
            });
    assertThat(days.get(DayOfWeek.TUESDAY).entries())
        .anySatisfy(
            entry -> {
              assertThat(entry.kind()).isEqualTo("STRENGTH");
              assertThat(entry.id()).isEqualTo("STRENGTH:PUSH");
              assertThat(entry.workoutType()).isEqualTo("PUSH");
              assertThat(entry.bodyView()).isEqualTo(BodyView.FRONT);
            });
    assertThat(days.get(DayOfWeek.THURSDAY).entries())
        .anySatisfy(entry -> assertThat(entry.bodyView()).isEqualTo(BodyView.BACK));
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
    statusRepository.upsertStatus(
        USER_ID,
        THIS_WEEK,
        "RUNNING:LONG_RUN",
        SessionStatus.COMPLETED,
        MONDAY,
        "Buenas sensaciones");

    TrainingEntry saturdayRun = entry(DayOfWeek.SATURDAY, "RUNNING:LONG_RUN");

    assertThat(saturdayRun.status()).isEqualTo("COMPLETED");
    assertThat(saturdayRun.notes()).isEqualTo("Buenas sensaciones");
  }

  /** The bug this whole change exists for: a completion must not leak into the following week. */
  @Test
  void ignoresStatusRecordedInAnEarlierWeek() {
    statusRepository.upsertStatus(
        USER_ID,
        LAST_WEEK,
        "RUNNING:LONG_RUN",
        SessionStatus.COMPLETED,
        Instant.parse("2026-08-15T09:00:00Z"),
        "la semana pasada");

    assertThat(service.currentWeek().days())
        .allSatisfy(
            day ->
                assertThat(day.entries())
                    .allSatisfy(entry -> assertThat(entry.status()).isEqualTo("PLANNED")));
  }

  @Test
  void movesASessionToTheDayStoredForThisWeek() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(byDay().get(DayOfWeek.MONDAY).entries())
        .extracting(TrainingEntry::id)
        .contains("STRENGTH:PUSH");
    assertThat(byDay().get(DayOfWeek.TUESDAY).entries())
        .extracting(TrainingEntry::id)
        .doesNotContain("STRENGTH:PUSH");
  }

  /** Two sessions on one day is a normal week, not an error — a day is a list of entries. */
  @Test
  void letsTwoSessionsShareADay() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(byDay().get(DayOfWeek.MONDAY).entries())
        .extracting(TrainingEntry::id)
        .containsExactly("RUNNING:EASY", "STRENGTH:PUSH");
  }

  @Test
  void movesASessionOntoAnOtherwiseRestDay() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "RUNNING:EASY", DayOfWeek.FRIDAY);

    assertThat(byDay().get(DayOfWeek.FRIDAY).isRest()).isFalse();
    assertThat(byDay().get(DayOfWeek.MONDAY).isRest()).isTrue();
  }

  /** A move is scoped to its week too: next Monday the plan is back on its policy days. */
  @Test
  void forgetsLastWeeksMove() {
    statusRepository.upsertScheduledDay(USER_ID, LAST_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(byDay().get(DayOfWeek.TUESDAY).entries())
        .extracting(TrainingEntry::id)
        .contains("STRENGTH:PUSH");
  }

  @Test
  void resolvesTheWeekStartToItsMondayFromAnyDayOfTheWeek() {
    // Thursday of the same week resolves to the same Monday, so a status written on Thursday is
    // read back on Saturday rather than landing in a bucket of its own.
    assertThat(serviceAt(Instant.parse("2026-08-20T23:30:00Z")).currentWeekStart())
        .isEqualTo(THIS_WEEK);
    assertThat(serviceAt(MONDAY).currentWeekStart()).isEqualTo(THIS_WEEK);
    assertThat(serviceAt(Instant.parse("2026-08-23T22:00:00Z")).currentWeekStart())
        .isEqualTo(THIS_WEEK);
  }
}

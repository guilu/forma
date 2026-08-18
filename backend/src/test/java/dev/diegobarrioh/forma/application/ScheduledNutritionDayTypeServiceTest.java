package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ScheduledNutritionDayTypeService}: the nutrition day type follows the
 * training calendar the user actually has, including any session moved this week, instead of the
 * fixed weekday policy (no Spring — ADR-007).
 */
class ScheduledNutritionDayTypeServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026. */
  private static final Instant MONDAY_MORNING = Instant.parse("2026-08-17T08:00:00Z");

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 18);
  private static final LocalDate FRIDAY = LocalDate.of(2026, 8, 21);
  private static final LocalDate THIS_WEEK = MONDAY;

  private final FakeTrainingSessionStatusRepository statusRepository =
      new FakeTrainingSessionStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(),
          new WorkoutTemplateService(),
          statusRepository,
          () -> USER_ID,
          Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC));
  private final ScheduledNutritionDayTypeService service =
      new ScheduledNutritionDayTypeService(
          scheduleService, Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC));

  @Test
  void matchesThePolicyWhenNothingHasBeenMoved() {
    assertThat(service.resolve(MONDAY)).isEqualTo(NutritionDayType.RUNNING);
    assertThat(service.resolve(TUESDAY)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(service.resolve(FRIDAY)).isEqualTo(NutritionDayType.REST);
  }

  @Test
  void followsASessionMovedToAnotherDay() {
    // Monday's easy run goes to Tuesday, Tuesday's push comes to Monday.
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "RUNNING:EASY", DayOfWeek.TUESDAY);
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    // The target follows the training actually planned, not the weekday it usually falls on.
    assertThat(service.resolve(MONDAY)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(service.resolve(TUESDAY)).isEqualTo(NutritionDayType.RUNNING);
  }

  @Test
  void turnsAnEmptiedDayIntoRest() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "RUNNING:EASY", DayOfWeek.FRIDAY);

    assertThat(service.resolve(MONDAY)).isEqualTo(NutritionDayType.REST);
    assertThat(service.resolve(FRIDAY)).isEqualTo(NutritionDayType.RUNNING);
  }

  @Test
  void letsRunningWinWhenADayHoldsBothKinds() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    // Same precedence the shared policy documents for a day that is both.
    assertThat(service.resolve(MONDAY)).isEqualTo(NutritionDayType.RUNNING);
  }

  /**
   * Overrides only exist for the week the calendar composes, so any other date falls back to the
   * policy rather than having this week's moves projected onto it.
   */
  @Test
  void fallsBackToThePolicyOutsideTheCurrentWeek() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    LocalDate lastMonday = LocalDate.of(2026, 8, 10);
    LocalDate nextMonday = LocalDate.of(2026, 8, 24);
    assertThat(service.resolve(lastMonday)).isEqualTo(NutritionDayType.RUNNING);
    assertThat(service.resolve(nextMonday)).isEqualTo(NutritionDayType.RUNNING);
  }
}

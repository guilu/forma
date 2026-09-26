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
  private final FakePlanAcceptanceRepository acceptanceRepository = acceptedAtMonday();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(),
          new WorkoutTemplateService(),
          statusRepository,
          () -> USER_ID,
          Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC),
          acceptanceRepository);

  /** Accepted this same Monday, so the derived week is always 1 -> ACTIVE (D1). */
  private static FakePlanAcceptanceRepository acceptedAtMonday() {
    FakePlanAcceptanceRepository repository = new FakePlanAcceptanceRepository();
    repository.markAccepted(USER_ID, MONDAY_MORNING);
    return repository;
  }

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
   * plan's own progress for that week (FIX1) rather than having this week's moves projected onto it
   * — a future week still active keeps following the weekday policy.
   */
  @Test
  void outsideCurrentWeekIgnoresThisWeeksOverrideWhenThePlanIsStillActive() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    LocalDate nextMonday = LocalDate.of(2026, 8, 24);
    assertThat(service.resolve(nextMonday)).isEqualTo(NutritionDayType.RUNNING);
  }

  /**
   * FIX1: a date whose week is before the account accepted the plan is {@code NotStarted}, hence
   * REST — not the weekday policy's answer, and not this week's override either.
   */
  @Test
  void outsideCurrentWeekBeforeAcceptanceIsRestRegardlessOfWeekdayOrOverrides() {
    statusRepository.upsertScheduledDay(USER_ID, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    LocalDate lastMonday = LocalDate.of(2026, 8, 10);
    assertThat(service.resolve(lastMonday)).isEqualTo(NutritionDayType.REST);
  }

  /** FIX1: a plan that was never accepted is REST on any date — current, past or future week. */
  @Test
  void notStartedPlanIsRestOnAnyDate() {
    ScheduledNutritionDayTypeService neverAccepted =
        new ScheduledNutritionDayTypeService(
            new WeeklyTrainingScheduleService(
                new RunningPlanService(),
                new WorkoutTemplateService(),
                new FakeTrainingSessionStatusRepository(),
                () -> USER_ID,
                Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC),
                new FakePlanAcceptanceRepository()),
            Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC));

    LocalDate lastMonday = LocalDate.of(2026, 8, 10);
    LocalDate nextMonday = LocalDate.of(2026, 8, 24);
    assertThat(neverAccepted.resolve(MONDAY)).isEqualTo(NutritionDayType.REST);
    assertThat(neverAccepted.resolve(lastMonday)).isEqualTo(NutritionDayType.REST);
    assertThat(neverAccepted.resolve(nextMonday)).isEqualTo(NutritionDayType.REST);
  }

  /**
   * FIX1/D4: once the 16-week plan is behind the account, running drops to rest but strength
   * survives — in the current week, where the calendar composes it directly...
   */
  @Test
  void completedPlanKeepsStrengthButDropsRunningInTheCurrentWeek() {
    ScheduledNutritionDayTypeService completed = completedPlanService();

    assertThat(completed.resolve(MONDAY)).isEqualTo(NutritionDayType.REST);
    assertThat(completed.resolve(TUESDAY)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(completed.resolve(FRIDAY)).isEqualTo(NutritionDayType.REST);
  }

  /** ...and (FIX1) outside it too, in both a past and a future week that are also completed. */
  @Test
  void completedPlanAppliesTheSameRuleOutsideTheCurrentWeek() {
    ScheduledNutritionDayTypeService completed = completedPlanService();

    LocalDate pastWeekRunningDay = LocalDate.of(2026, 8, 15); // Saturday, week of 2026-08-10
    LocalDate pastWeekStrengthDay = LocalDate.of(2026, 8, 11); // Tuesday, week of 2026-08-10
    LocalDate futureWeekRunningDay = LocalDate.of(2026, 8, 24); // Monday, week of 2026-08-24
    LocalDate futureWeekStrengthDay = LocalDate.of(2026, 8, 25); // Tuesday, week of 2026-08-24

    assertThat(completed.resolve(pastWeekRunningDay)).isEqualTo(NutritionDayType.REST);
    assertThat(completed.resolve(pastWeekStrengthDay)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(completed.resolve(futureWeekRunningDay)).isEqualTo(NutritionDayType.REST);
    assertThat(completed.resolve(futureWeekStrengthDay)).isEqualTo(NutritionDayType.STRENGTH);
  }

  /** Accepted 2026-04-20 (Monday), 17 weeks before "now" (2026-08-17) — past the 16-week plan. */
  private static ScheduledNutritionDayTypeService completedPlanService() {
    FakePlanAcceptanceRepository longAgo = new FakePlanAcceptanceRepository();
    longAgo.markAccepted(USER_ID, Instant.parse("2026-04-20T08:00:00Z"));
    return new ScheduledNutritionDayTypeService(
        new WeeklyTrainingScheduleService(
            new RunningPlanService(),
            new WorkoutTemplateService(),
            new FakeTrainingSessionStatusRepository(),
            () -> USER_ID,
            Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC),
            longAgo),
        Clock.fixed(MONDAY_MORNING, ZoneOffset.UTC));
  }
}

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrainingSessionRescheduleService} (V60): moving a session to another day of
 * the current week (no Spring — ADR-007).
 *
 * <p>Asserts through {@link WeeklyTrainingScheduleService#currentWeek()} rather than the
 * repository, because "the session moved" is a statement about the calendar the user sees, not
 * about a row.
 */
class TrainingSessionRescheduleServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026 — Diego's example week. */
  private static final Instant MONDAY = Instant.parse("2026-08-17T08:00:00Z");

  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);

  private final Clock clock = Clock.fixed(MONDAY, ZoneOffset.UTC);
  private final FakeTrainingSessionStatusRepository repository =
      new FakeTrainingSessionStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), repository, () -> USER_ID, clock);
  private final TrainingSessionRescheduleService service =
      new TrainingSessionRescheduleService(scheduleService, repository, () -> USER_ID);

  private Map<DayOfWeek, TrainingDay> byDay() {
    return scheduleService.currentWeek().days().stream()
        .collect(Collectors.toMap(TrainingDay::dayOfWeek, Function.identity()));
  }

  private List<String> idsOn(DayOfWeek day) {
    return byDay().get(day).entries().stream().map(TrainingEntry::id).toList();
  }

  @Test
  void movesASessionToAnotherDay() {
    service.reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(idsOn(DayOfWeek.MONDAY)).contains("STRENGTH:PUSH");
    assertThat(idsOn(DayOfWeek.TUESDAY)).doesNotContain("STRENGTH:PUSH");
  }

  /**
   * Diego's case: he ran on Sunday, so on Monday he wants to do Tuesday's push and push the easy
   * run to Tuesday. Two moves, no special "swap" operation.
   */
  @Test
  void swappingTwoDaysIsJustTwoMoves() {
    service.reschedule("RUNNING:EASY", DayOfWeek.TUESDAY);
    service.reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(idsOn(DayOfWeek.MONDAY)).containsExactly("STRENGTH:PUSH");
    assertThat(idsOn(DayOfWeek.TUESDAY)).containsExactly("RUNNING:EASY");
  }

  @Test
  void keepsAnAlreadyRecordedStatusWhenTheSessionMoves() {
    repository.upsertStatus(
        USER_ID, THIS_WEEK, "STRENGTH:PUSH", SessionStatus.COMPLETED, MONDAY, "hecho");

    service.reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);

    TrainingEntry moved =
        byDay().get(DayOfWeek.MONDAY).entries().stream()
            .filter(entry -> entry.id().equals("STRENGTH:PUSH"))
            .findFirst()
            .orElseThrow();
    // Moving a session is not un-doing it.
    assertThat(moved.status()).isEqualTo("COMPLETED");
    assertThat(moved.notes()).isEqualTo("hecho");
  }

  @Test
  void aNullDayRestoresThePlannedDay() {
    service.reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);
    service.reschedule("STRENGTH:PUSH", null);

    assertThat(idsOn(DayOfWeek.TUESDAY)).contains("STRENGTH:PUSH");
    assertThat(idsOn(DayOfWeek.MONDAY)).doesNotContain("STRENGTH:PUSH");
  }

  @Test
  void writesTheMoveIntoTheCurrentWeekOnly() {
    service.reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);

    assertThat(repository.findByUserAndWeek(USER_ID, LocalDate.of(2026, 8, 10))).isEmpty();
    assertThat(repository.findByUserAndWeek(USER_ID, THIS_WEEK).get("STRENGTH:PUSH").scheduledDay())
        .isEqualTo(DayOfWeek.MONDAY);
  }

  @Test
  void rejectsUnknownSessionId() {
    assertThatThrownBy(() -> service.reschedule("RUNNING:NOPE", DayOfWeek.FRIDAY))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("RUNNING:NOPE");
  }

  /** The pre-V60 ids named a day; they must not quietly resolve to anything now. */
  @Test
  void rejectsAPreV60DayKeyedSessionId() {
    assertThatThrownBy(() -> service.reschedule("MONDAY:RUNNING", DayOfWeek.FRIDAY))
        .isInstanceOf(NotFoundException.class);
  }
}

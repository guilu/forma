package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.LoggedSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrainingSetLogService} (training-progression-and-logging slice B, design
 * D6): resolves a real week session to its strength template exactly like {@link
 * MuscleWorkedMapService} and {@link TrainingSessionStatusService} (no duplicated resolution
 * logic), then validates set writes against that template and reconciles reads against it (no
 * Spring — ADR-007).
 */
class TrainingSetLogServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026 — the week every write below lands in. */
  private static final Instant NOW = Instant.parse("2026-08-17T18:45:00Z");

  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final FakeTrainingSetLogRepository repository = new FakeTrainingSetLogRepository();
  private final FakePlanAcceptanceRepository acceptanceRepository = acceptedAtMonday();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(),
          new WorkoutTemplateService(),
          new FakeTrainingSessionStatusRepository(),
          () -> USER_ID,
          clock,
          acceptanceRepository);

  /** Accepted the same Monday {@link #NOW} falls in, so the derived week is always 1 -> ACTIVE (D1). */
  private static FakePlanAcceptanceRepository acceptedAtMonday() {
    FakePlanAcceptanceRepository repository = new FakePlanAcceptanceRepository();
    repository.markAccepted(USER_ID, NOW);
    return repository;
  }

  private final TrainingSetLogService service =
      new TrainingSetLogService(
          scheduleService, new WorkoutTemplateService(), repository, () -> USER_ID, clock);

  @Test
  void aFreshSessionReturnsTheFullTemplateGridWithEmptySets() {
    TrainingSetLogView view = service.getSets("STRENGTH:PUSH");

    assertThat(view.sessionId()).isEqualTo("STRENGTH:PUSH");
    assertThat(view.sets())
        .contains(new LoggedSet("push-up", 1, null, null, false));
  }

  @Test
  void writingASetIsReflectedOnTheNextRead() {
    LoggedSet written = service.putSet("STRENGTH:PUSH", "push-up", 1, 60.0, 8, true);

    assertThat(written).isEqualTo(new LoggedSet("push-up", 1, 60.0, 8, true));
    assertThat(service.getSets("STRENGTH:PUSH").sets()).contains(written);
    assertThat(repository.findByUserWeekAndSession(USER_ID, THIS_WEEK, "STRENGTH:PUSH"))
        .contains(written);
  }

  @Test
  void aRunningSessionIsRejectedOnReadAndWrite() {
    assertThatThrownBy(() -> service.getSets("RUNNING:LONG_RUN"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("RUNNING:LONG_RUN");
    assertThatThrownBy(
            () -> service.putSet("RUNNING:LONG_RUN", "push-up", 1, 60.0, 8, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("RUNNING:LONG_RUN");
  }

  @Test
  void aSessionOutsideTheCurrentWeekIsRejected() {
    // FULL_BODY has no template on any day of the week (same fixture as MuscleWorkedMapServiceTest).
    assertThatThrownBy(() -> service.getSets("STRENGTH:FULL_BODY"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("STRENGTH:FULL_BODY");
  }

  @Test
  void writingASetTheTemplateDoesNotPrescribeIsRejected() {
    // Way beyond any real template's set count.
    assertThatThrownBy(
            () -> service.putSet("STRENGTH:PUSH", "push-up", 99, 60.0, 8, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("push-up");
    assertThatThrownBy(
            () -> service.putSet("STRENGTH:PUSH", "not-a-real-exercise", 1, 60.0, 8, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not-a-real-exercise");
  }

  @Test
  void anOrphanedSetIsNeitherRenderedNorDeleted() {
    // Written directly against the repository, bypassing the template validation putSet enforces —
    // simulating a row left behind by a template that has since dropped this exerciseId (design D6).
    repository.upsertSet(
        USER_ID,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("not-a-real-exercise", 1, 50.0, 10, true),
        Instant.now(clock));

    TrainingSetLogView view = service.getSets("STRENGTH:PUSH");

    assertThat(view.sets()).extracting(LoggedSet::exerciseId).doesNotContain("not-a-real-exercise");
    // Not rendered is not the same as deleted: the row is still there.
    assertThat(repository.findByUserWeekAndSession(USER_ID, THIS_WEEK, "STRENGTH:PUSH"))
        .extracting(LoggedSet::exerciseId)
        .contains("not-a-real-exercise");
  }
}

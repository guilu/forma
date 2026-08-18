package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrainingSessionStatusService} (FOR-27): marks a real week session, stamps
 * when it was done, and rejects an unknown id (no Spring — ADR-007).
 */
class TrainingSessionStatusServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Monday 17 August 2026 — the week every write below lands in. */
  private static final Instant NOW = Instant.parse("2026-08-17T18:45:00Z");

  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final FakeTrainingSessionStatusRepository repository =
      new FakeTrainingSessionStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), repository, () -> USER_ID, clock);
  private final TrainingSessionStatusService service =
      new TrainingSessionStatusService(scheduleService, repository, () -> USER_ID, clock);

  private StoredSessionStatus storedThisWeek(String sessionKey) {
    return repository.findByUserAndWeek(USER_ID, THIS_WEEK).get(sessionKey);
  }

  @Test
  void marksAnExistingSessionCompletedWithNotes() {
    StoredSessionStatus result =
        service.updateStatus("RUNNING:LONG_RUN", SessionStatus.COMPLETED, "Hecho");

    assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
    assertThat(storedThisWeek("RUNNING:LONG_RUN").notes()).isEqualTo("Hecho");
  }

  @Test
  void marksAStrengthSessionSkipped() {
    service.updateStatus("STRENGTH:PUSH", SessionStatus.SKIPPED, null);

    assertThat(storedThisWeek("STRENGTH:PUSH").status()).isEqualTo(SessionStatus.SKIPPED);
  }

  @Test
  void stampsWhenACompletionActuallyHappened() {
    service.updateStatus("RUNNING:EASY", SessionStatus.COMPLETED, null);

    // The real moment, not the day the plan plans it for: those differ exactly when someone trains
    // a day late, which is the case that motivated moving sessions at all.
    assertThat(storedThisWeek("RUNNING:EASY").completedAt()).isEqualTo(NOW);
  }

  @Test
  void clearsTheCompletionMomentWhenASessionIsUncompleted() {
    service.updateStatus("RUNNING:EASY", SessionStatus.COMPLETED, null);
    service.updateStatus("RUNNING:EASY", SessionStatus.PLANNED, null);

    // Otherwise a session would read as "done at some point" while its status says otherwise.
    assertThat(storedThisWeek("RUNNING:EASY").completedAt()).isNull();
    assertThat(storedThisWeek("RUNNING:EASY").status()).isEqualTo(SessionStatus.PLANNED);
  }

  @Test
  void writesIntoTheCurrentWeekOnly() {
    service.updateStatus("RUNNING:EASY", SessionStatus.COMPLETED, null);

    assertThat(repository.findByUserAndWeek(USER_ID, LocalDate.of(2026, 8, 10))).isEmpty();
    assertThat(repository.findByUserAndWeek(USER_ID, THIS_WEEK)).containsKey("RUNNING:EASY");
  }

  @Test
  void rejectsUnknownSessionId() {
    assertThatThrownBy(() -> service.updateStatus("SUNDAY:RUNNING", SessionStatus.COMPLETED, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("SUNDAY:RUNNING");
  }
}

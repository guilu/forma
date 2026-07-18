package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrainingSessionStatusService} (FOR-27): marks a real week session and
 * rejects an unknown id (no Spring — ADR-007).
 */
class TrainingSessionStatusServiceTest {

  private final FakeStatusRepository repository = new FakeStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), repository);
  private final TrainingSessionStatusService service =
      new TrainingSessionStatusService(scheduleService, repository);

  @Test
  void marksAnExistingSessionCompletedWithNotes() {
    StoredSessionStatus result =
        service.updateStatus("SATURDAY:RUNNING", SessionStatus.COMPLETED, "Hecho");

    assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
    assertThat(repository.findAll().get("SATURDAY:RUNNING").notes()).isEqualTo("Hecho");
  }

  @Test
  void marksAStrengthSessionSkipped() {
    service.updateStatus("TUESDAY:STRENGTH", SessionStatus.SKIPPED, null);

    assertThat(repository.findAll().get("TUESDAY:STRENGTH").status())
        .isEqualTo(SessionStatus.SKIPPED);
  }

  @Test
  void rejectsUnknownSessionId() {
    assertThatThrownBy(() -> service.updateStatus("SUNDAY:RUNNING", SessionStatus.COMPLETED, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("SUNDAY:RUNNING");
  }

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

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for marking a training session's completion status (FOR-27).
 *
 * <p>Validates the target session id against the current week's schedule (so an unknown id yields a
 * {@link NotFoundException} → 404) and persists the status (and optional notes) via the repository
 * port. The transition rule is intentionally permissive for the MVP: any of {@code PLANNED}, {@code
 * COMPLETED}, {@code SKIPPED} can be set (including reverting), keeping completion simple.
 */
@Service
public class TrainingSessionStatusService {

  private final WeeklyTrainingScheduleService scheduleService;
  private final TrainingSessionStatusRepository repository;

  public TrainingSessionStatusService(
      WeeklyTrainingScheduleService scheduleService, TrainingSessionStatusRepository repository) {
    this.scheduleService = scheduleService;
    this.repository = repository;
  }

  /**
   * Records the status (and optional note) for a session in the current week.
   *
   * @throws NotFoundException if the id is not a session in the current week's schedule
   */
  public StoredSessionStatus updateStatus(String sessionId, SessionStatus status, String notes) {
    if (!currentSessionIds().contains(sessionId)) {
      throw new NotFoundException("No existe la sesión de entrenamiento: " + sessionId);
    }
    repository.upsert(sessionId, status, notes);
    return new StoredSessionStatus(sessionId, status, notes);
  }

  private Set<String> currentSessionIds() {
    return scheduleService.currentWeek().days().stream()
        .flatMap(day -> day.entries().stream())
        .map(TrainingEntry::id)
        .collect(Collectors.toSet());
  }
}

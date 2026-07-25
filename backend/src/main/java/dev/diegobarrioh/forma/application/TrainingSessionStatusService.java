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
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V31): this "gap table" service had ZERO
 * owner-scoping before this slice — {@code training_session_status}'s primary key was rebuilt to a
 * composite {@code (user_id, session_id)} (the bare {@code session_id} alone collided across
 * users). Resolves the caller's account id via {@link CurrentUserProvider} on every call.
 */
@Service
public class TrainingSessionStatusService {

  private final WeeklyTrainingScheduleService scheduleService;
  private final TrainingSessionStatusRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public TrainingSessionStatusService(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusRepository repository,
      CurrentUserProvider currentUserProvider) {
    this.scheduleService = scheduleService;
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
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
    repository.upsert(currentUserProvider.currentUserId(), sessionId, status, notes);
    return new StoredSessionStatus(sessionId, status, notes);
  }

  private Set<String> currentSessionIds() {
    return scheduleService.currentWeek().days().stream()
        .flatMap(day -> day.entries().stream())
        .map(TrainingEntry::id)
        .collect(Collectors.toSet());
  }
}

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.Instant;
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
 * <p>The write is scoped to the current week and stamps {@code completedAt} (V60). Recording *when*
 * a session was done is what lets the week reset on Monday instead of a completion leaking into
 * every following week, and it is deliberately the real moment rather than the day the session was
 * planned for — the two differ precisely when someone trains a day late, which is the case that
 * motivated moving sessions at all.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V31): resolves the caller's account id via
 * {@link CurrentUserProvider} on every call.
 */
@Service
public class TrainingSessionStatusService {

  private final WeeklyTrainingScheduleService scheduleService;
  private final TrainingSessionStatusRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public TrainingSessionStatusService(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusRepository repository,
      CurrentUserProvider currentUserProvider,
      Clock clock) {
    this.scheduleService = scheduleService;
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
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
    // Only a completion carries a moment; reverting to PLANNED or SKIPPED clears it, so a session
    // can never read as "completed at some point" while its status says otherwise.
    Instant completedAt = (status == SessionStatus.COMPLETED) ? Instant.now(clock) : null;
    repository.upsertStatus(
        currentUserProvider.currentUserId(),
        scheduleService.currentWeekStart(),
        sessionId,
        status,
        completedAt,
        notes);
    return new StoredSessionStatus(sessionId, status, null, completedAt, notes);
  }

  private Set<String> currentSessionIds() {
    return scheduleService.currentWeek().days().stream()
        .flatMap(day -> day.entries().stream())
        .map(TrainingEntry::id)
        .collect(Collectors.toSet());
  }
}

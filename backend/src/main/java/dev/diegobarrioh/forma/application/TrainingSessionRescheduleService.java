package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for moving a training session to another day of the current week (V60).
 *
 * <p>Real life does not follow the plan's weekdays: a run gets done a day late, or the week gets
 * rearranged around whatever else is happening. Before V60 that was unrepresentable — a session was
 * named after its day ({@code "MONDAY:RUNNING"}), so "move it to Tuesday" and "make it a different
 * session" were the same edit.
 *
 * <p>Moving one session is the only primitive offered, deliberately. Swapping two days is two moves
 * and shifting the whole week is six, so nothing here needs a rule for what happens to a session
 * pushed past Sunday — the caller decides that by choosing where each session lands.
 *
 * <p>Two sessions may share a day. The calendar models a day as a list of entries and always has,
 * so doubling up needs no special case; refusing it would be inventing a training rule the app has
 * no business enforcing (a run and a lift on the same day is a normal week).
 *
 * <p>The override lasts one week: rows are keyed by {@code (user, week_start, session_key)}, so
 * next Monday the plan is back on its policy days without anything having to undo the move.
 */
@Service
public class TrainingSessionRescheduleService {

  private final WeeklyTrainingScheduleService scheduleService;
  private final TrainingSessionStatusRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public TrainingSessionRescheduleService(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusRepository repository,
      CurrentUserProvider currentUserProvider) {
    this.scheduleService = scheduleService;
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Moves {@code sessionId} to {@code day} for the current week only.
   *
   * @param day the day to move it to, or {@code null} to restore its planned day
   * @throws NotFoundException if the id is not a session in the current week's schedule
   */
  public void reschedule(String sessionId, DayOfWeek day) {
    if (!currentSessionIds().contains(sessionId)) {
      throw new NotFoundException("No existe la sesión de entrenamiento: " + sessionId);
    }
    repository.upsertScheduledDay(
        currentUserProvider.currentUserId(), scheduleService.currentWeekStart(), sessionId, day);
  }

  private Set<String> currentSessionIds() {
    return scheduleService.currentWeek().days().stream()
        .flatMap(dayEntry -> dayEntry.entries().stream())
        .map(TrainingEntry::id)
        .collect(Collectors.toSet());
  }
}

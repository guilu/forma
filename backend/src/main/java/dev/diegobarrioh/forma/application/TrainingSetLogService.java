package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.LoggedSet;
import dev.diegobarrioh.forma.domain.StrengthWorkoutItem;
import dev.diegobarrioh.forma.domain.StrengthWorkoutTemplate;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for the per-set training log (training-progression-and-logging slice B,
 * design D6/D7).
 *
 * <p><b>No duplicated resolution logic:</b> the session id is validated against the real {@link
 * WeeklyTrainingScheduleService#currentWeek()} entries, exactly like {@link MuscleWorkedMapService}
 * and {@link TrainingSessionStatusService}. Unlike {@link MuscleWorkedMapService} (which returns an
 * empty map for a non-strength session), a {@code "RUNNING"} entry is rejected with {@link
 * NotFoundException} here: the set log has nothing meaningful to say about a session with no sets,
 * so both read and write 404 for it (spec training-set-log).
 *
 * <p><b>Reads are template-driven (design D6):</b> {@link #getSets(String)} returns one entry per
 * {@code (exerciseId, setNumber)} the current template prescribes, left-joined with any stored
 * {@link LoggedSet}. A row logged against an exerciseId the template no longer has (e.g. the
 * template was edited after the set was logged) is not rendered, but it is not deleted either —
 * only a fresh {@code hasSet} match ever surfaces a set again.
 *
 * <p><b>Writes are per-set (design D7):</b> {@link #putSet(String, String, int, Double, Integer,
 * boolean)} validates the target {@code (exerciseId, setNumber)} against {@link
 * StrengthWorkoutTemplate#hasSet(String, int)} before persisting, mirroring how {@link
 * TrainingSessionStatusService#updateStatus} validates the session id itself.
 *
 * <p>Real multi-user auth (ADR-012): resolves the caller's account id via {@link
 * CurrentUserProvider} on every call, and the current week via {@link Clock} (through {@link
 * WeeklyTrainingScheduleService#currentWeekStart()}), exactly like {@link
 * TrainingSessionStatusService}.
 */
@Service
public class TrainingSetLogService {

  private static final String STRENGTH_KIND = "STRENGTH";

  private final WeeklyTrainingScheduleService scheduleService;
  private final WorkoutTemplateService workoutTemplateService;
  private final TrainingSetLogRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public TrainingSetLogService(
      WeeklyTrainingScheduleService scheduleService,
      WorkoutTemplateService workoutTemplateService,
      TrainingSetLogRepository repository,
      CurrentUserProvider currentUserProvider,
      Clock clock) {
    this.scheduleService = scheduleService;
    this.workoutTemplateService = workoutTemplateService;
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
  }

  /**
   * The current week's full set grid for {@code sessionId}, template-driven with the log
   * left-joined.
   *
   * @throws NotFoundException if the id is not a strength session in the current week's schedule
   */
  public TrainingSetLogView getSets(String sessionId) {
    StrengthWorkoutTemplate template = template(resolveStrengthSessionType(sessionId));
    UUID userId = currentUserProvider.currentUserId();
    LocalDate weekStart = scheduleService.currentWeekStart();
    Map<String, LoggedSet> stored =
        repository.findByUserWeekAndSession(userId, weekStart, sessionId).stream()
            .collect(Collectors.toMap(this::gridKey, set -> set));

    List<LoggedSet> sets = new ArrayList<>();
    for (StrengthWorkoutItem item : template.items()) {
      for (int setNumber = 1; setNumber <= item.sets(); setNumber++) {
        LoggedSet found = stored.get(item.exerciseId() + ":" + setNumber);
        sets.add(
            found != null ? found : new LoggedSet(item.exerciseId(), setNumber, null, null, false));
      }
    }
    return new TrainingSetLogView(sessionId, List.copyOf(sets));
  }

  /**
   * Records one set for {@code sessionId}, in the current week.
   *
   * @throws NotFoundException if the session id is not a strength session in the current week's
   *     schedule, or the current template does not prescribe {@code setNumber} for {@code
   *     exerciseId}
   */
  public LoggedSet putSet(
      String sessionId,
      String exerciseId,
      int setNumber,
      Double weightKg,
      Integer reps,
      boolean done) {
    StrengthWorkoutTemplate template = template(resolveStrengthSessionType(sessionId));
    if (!template.hasSet(exerciseId, setNumber)) {
      throw new NotFoundException(
          "La plantilla actual no prescribe la serie " + setNumber + " de " + exerciseId);
    }

    LoggedSet set = new LoggedSet(exerciseId, setNumber, weightKg, reps, done);
    repository.upsertSet(
        currentUserProvider.currentUserId(),
        scheduleService.currentWeekStart(),
        sessionId,
        set,
        Instant.now(clock));
    return set;
  }

  private String gridKey(LoggedSet set) {
    return set.exerciseId() + ":" + set.setNumber();
  }

  private WorkoutType resolveStrengthSessionType(String sessionId) {
    for (TrainingDay day : scheduleService.currentWeek().days()) {
      for (TrainingEntry entry : day.entries()) {
        if (entry.id().equals(sessionId)) {
          if (!STRENGTH_KIND.equals(entry.kind())) {
            throw new NotFoundException(
                "El registro de series no está disponible para sesiones de running: " + sessionId);
          }
          return WorkoutType.valueOf(entry.workoutType());
        }
      }
    }
    throw new NotFoundException("No existe la sesión de entrenamiento: " + sessionId);
  }

  private StrengthWorkoutTemplate template(WorkoutType type) {
    return workoutTemplateService
        .findByType(type)
        .orElseThrow(
            () -> new IllegalStateException("no workout template for strength type: " + type));
  }
}

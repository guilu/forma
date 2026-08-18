package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.Exercise;
import dev.diegobarrioh.forma.domain.MuscleLoad;
import dev.diegobarrioh.forma.domain.StrengthWorkoutItem;
import dev.diegobarrioh.forma.domain.StrengthWorkoutTemplate;
import dev.diegobarrioh.forma.domain.WeeklyTrainingDayPolicy;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Application use case for the muscle-worked-map read model (FOR-136, muscle-worked-map slice of
 * FOR-104): resolves a session id to its strength template's exercises and aggregates their {@code
 * primaryMuscles} into a muscle -&gt; {@link MuscleLoad} map. Pure derivation — no new persistence,
 * no catalog extension (spec FOR-136 NFR).
 *
 * <p><b>Session id scheme (resolved Open Question, spec FOR-136; re-keyed by V60):</b> the FOR-26
 * weekly-schedule stable id ({@code "STRENGTH:<TYPE>"}, e.g. {@code "STRENGTH:PUSH"}), the same id
 * the FOR-27 {@link TrainingSessionStatusService} already validates against and the id the frontend
 * already has from {@code GET /training/week}. Chosen over the workout-template id so the same id
 * consistently identifies "this week's session" across the training API, and so this service can
 * reuse {@link WeeklyTrainingScheduleService#currentWeek()} for id validation instead of
 * duplicating it.
 *
 * <p><b>No duplicated resolution logic (spec FOR-136 NFR):</b> the session id is validated against
 * the real {@link WeeklyTrainingScheduleService#currentWeek()} entries (exactly like {@link
 * TrainingSessionStatusService}), the {@link WorkoutType} is read off the entry the schedule
 * already resolved (rather than re-derived from the day, which stopped being the session's identity
 * in V60 and can now be overridden by a reschedule), the template lookup reuses {@link
 * WorkoutTemplateService} (FOR-25), and each exercise lookup reuses {@link ExerciseCatalogService}
 * (FOR-24).
 *
 * <p>An unknown session id (not in the current week's schedule) throws {@link NotFoundException}
 * (404). A known but non-strength session (a {@code "RUNNING"} entry) returns an empty map (200),
 * never an error (spec Edge Cases). <b>Documented assumption:</b> a pure rest day (e.g. Sunday
 * under the current MVP policy, see {@link WeeklyTrainingDayPolicy}) has no entry at all in the
 * schedule — the app has never generated a stable id for a day with nothing planned — so there is
 * no real "rest day id" to query; such a request falls through the same "unknown session id -&gt;
 * 404" path as any other unrecognized id, rather than inventing a new id scheme for rest days.
 */
@Service
public class MuscleWorkedMapService {

  private static final String STRENGTH_KIND = "STRENGTH";

  private final WeeklyTrainingScheduleService scheduleService;
  private final WorkoutTemplateService workoutTemplateService;
  private final ExerciseCatalogService exerciseCatalogService;

  public MuscleWorkedMapService(
      WeeklyTrainingScheduleService scheduleService,
      WorkoutTemplateService workoutTemplateService,
      ExerciseCatalogService exerciseCatalogService) {
    this.scheduleService = scheduleService;
    this.workoutTemplateService = workoutTemplateService;
    this.exerciseCatalogService = exerciseCatalogService;
  }

  /**
   * Resolves the muscle-worked map for {@code sessionId}.
   *
   * @throws NotFoundException if the id is not a session in the current week's schedule
   */
  public MuscleWorkedMap resolve(String sessionId) {
    for (TrainingDay day : scheduleService.currentWeek().days()) {
      for (TrainingEntry entry : day.entries()) {
        if (entry.id().equals(sessionId)) {
          return STRENGTH_KIND.equals(entry.kind())
              ? aggregate(sessionId, WorkoutType.valueOf(entry.workoutType()))
              : new MuscleWorkedMap(sessionId, List.of());
        }
      }
    }
    throw new NotFoundException("No existe la sesión de entrenamiento: " + sessionId);
  }

  private MuscleWorkedMap aggregate(String sessionId, WorkoutType type) {
    StrengthWorkoutTemplate template =
        workoutTemplateService
            .findByType(type)
            .orElseThrow(
                () -> new IllegalStateException("no workout template for strength type: " + type));

    // LinkedHashMap: deterministic output order (first-appearance across the template's items).
    Map<String, Integer> frequencyByMuscle = new LinkedHashMap<>();
    for (StrengthWorkoutItem item : template.items()) {
      Exercise exercise =
          exerciseCatalogService
              .findById(item.exerciseId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "unknown catalog exerciseId: " + item.exerciseId()));
      for (String muscle : exercise.primaryMuscles()) {
        frequencyByMuscle.merge(muscle, 1, Integer::sum);
      }
    }

    List<MuscleWorked> muscles =
        frequencyByMuscle.entrySet().stream()
            .map(e -> new MuscleWorked(e.getKey(), MuscleLoad.fromFrequency(e.getValue())))
            .toList();
    return new MuscleWorkedMap(sessionId, muscles);
  }
}

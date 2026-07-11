package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.domain.ExerciseCatalog;
import dev.diegobarrioh.forma.domain.StrengthWorkoutTemplate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/workouts} and {@code GET
 * /api/v1/training/workouts/{type}} (FOR-99).
 *
 * <p>Delivery read model, distinct from the application/domain {@link StrengthWorkoutTemplate}
 * (ADR-005 — controllers never return application/domain types directly). Each item resolves its
 * exercise name from the FOR-24 {@link ExerciseCatalog} so the training UI (FOR-53) can render a
 * real exercise list instead of a bare id; falls back to the exercise id if unresolved (should not
 * happen — catalog integrity is enforced at build by {@code WorkoutTemplateCatalog}). Deliberately
 * lean: no primary muscles/equipment/instructions, no progression or scheduling fields.
 */
public record WorkoutResponse(String workoutType, List<Item> items) {

  public record Item(
      String exerciseId,
      String exerciseName,
      int order,
      int sets,
      int repsMin,
      int repsMax,
      int restSeconds,
      int rir) {}

  /** Maps a strength workout template to its API read model, resolving exercise names. */
  public static WorkoutResponse from(StrengthWorkoutTemplate template) {
    List<Item> items =
        template.items().stream()
            .map(
                item ->
                    new Item(
                        item.exerciseId(),
                        ExerciseCatalog.findById(item.exerciseId())
                            .map(exercise -> exercise.name())
                            .orElse(item.exerciseId()),
                        item.order(),
                        item.sets(),
                        item.repsMin(),
                        item.repsMax(),
                        item.restSeconds(),
                        item.rir()))
            .toList();
    return new WorkoutResponse(template.workoutType().name(), items);
  }
}

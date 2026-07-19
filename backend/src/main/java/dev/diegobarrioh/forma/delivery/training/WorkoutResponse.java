package dev.diegobarrioh.forma.delivery.training;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 *
 * <p><b>FOR-154:</b> {@code repScheme} plus its optional bounds mirror the domain {@link
 * dev.diegobarrioh.forma.domain.RepScheme} extension so AMRAP and timed-hold items can be
 * represented without lying about {@code repsMin}/{@code repsMax}. Fields that do not apply to the
 * item's scheme are {@code null} and omitted from the JSON body ({@link
 * JsonInclude.Include#NON_NULL}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkoutResponse(String workoutType, List<Item> items) {

  public record Item(
      String exerciseId,
      String exerciseName,
      int order,
      int sets,
      String repScheme,
      Integer repsMin,
      Integer repsMax,
      Integer durationSecondsMin,
      Integer durationSecondsMax,
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
                        item.repScheme().name(),
                        item.repsMin(),
                        item.repsMax(),
                        item.durationSecondsMin(),
                        item.durationSecondsMax(),
                        item.restSeconds(),
                        item.rir()))
            .toList();
    return new WorkoutResponse(template.workoutType().name(), items);
  }
}

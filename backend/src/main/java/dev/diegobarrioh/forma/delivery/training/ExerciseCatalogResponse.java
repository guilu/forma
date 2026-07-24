package dev.diegobarrioh.forma.delivery.training;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.CatalogExercise;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/exercises} and {@code GET
 * /api/v1/training/exercises/{id}} (FOR-172).
 *
 * <p>Delivery read model, distinct from the application {@link CatalogExercise} (ADR-005 —
 * controllers never return application/domain types directly). Modality-inapplicable fields stay
 * {@code null} and are omitted from the JSON body ({@link JsonInclude.Include#NON_NULL}) rather
 * than fabricated — mirrors {@link CatalogExercise}'s own nullability. {@code muscles} is omitted
 * for RUNNING items (always empty there) instead of serialized as {@code []}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExerciseCatalogResponse(
    String id,
    String name,
    String modality,
    String movementPattern,
    String equipment,
    Integer defaultSets,
    String defaultReps,
    BigDecimal defaultDistanceKm,
    String defaultPaceMinPerKm,
    String sessionKind,
    String instructions,
    List<String> muscles) {

  /** Maps a persisted catalog exercise to its API read model. */
  public static ExerciseCatalogResponse from(CatalogExercise exercise) {
    return new ExerciseCatalogResponse(
        exercise.id(),
        exercise.name(),
        exercise.modality().name(),
        exercise.movementPattern(),
        exercise.equipment(),
        exercise.defaultSets(),
        exercise.defaultReps(),
        exercise.defaultDistanceKm(),
        exercise.defaultPaceMinPerKm(),
        exercise.sessionKind(),
        exercise.instructions(),
        exercise.muscles().isEmpty() ? null : exercise.muscles());
  }
}

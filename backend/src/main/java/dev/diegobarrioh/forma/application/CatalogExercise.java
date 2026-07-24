package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Modality;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read model for a persisted {@code exercise_catalog} row (FOR-172, ADR-011).
 *
 * <p>Deliberately separate from {@link dev.diegobarrioh.forma.domain.Exercise}: {@code Exercise}
 * enforces STRENGTH-only invariants (non-null movement pattern/equipment, non-empty muscles,
 * non-blank instructions) that legitimately do not hold for RUNNING catalog rows. This record stays
 * lenient so it can faithfully represent nullable columns across modalities without coupling the
 * persisted catalog to the static strength domain type.
 *
 * @param id stable catalog id (verbatim from {@code domain.ExerciseCatalog} for STRENGTH, {@code
 *     "running-" + SessionType} for RUNNING)
 * @param name human-readable name
 * @param modality STRENGTH or RUNNING
 * @param movementPattern STRENGTH only; {@code null} for RUNNING
 * @param equipment STRENGTH only; {@code null} for RUNNING
 * @param defaultSets STRENGTH prescription hint; {@code null} in FOR-172 (FOR-175 territory)
 * @param defaultReps STRENGTH prescription hint; {@code null} in FOR-172
 * @param defaultDistanceKm RUNNING prescription hint; {@code null} in FOR-172
 * @param defaultPaceMinPerKm RUNNING prescription hint; {@code null} in FOR-172
 * @param sessionKind RUNNING only, verbatim {@code SessionType.name()}; {@code null} for STRENGTH
 * @param instructions STRENGTH only authored (ES) cues; {@code null} for RUNNING
 * @param muscles ordered primary muscles (empty for RUNNING)
 */
public record CatalogExercise(
    String id,
    String name,
    Modality modality,
    String movementPattern,
    String equipment,
    Integer defaultSets,
    String defaultReps,
    BigDecimal defaultDistanceKm,
    String defaultPaceMinPerKm,
    String sessionKind,
    String instructions,
    List<String> muscles) {}

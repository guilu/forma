package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Objects;

/**
 * A strength exercise definition (FOR-24) — reference data, not user data.
 *
 * <p>Framework-free domain type (ADR-001), per docs/domain-model.md's "Exercise". Exercises are
 * composed by strength workout templates (FOR-25), which reference them by {@link #id}. Only
 * home-friendly {@link Equipment} is allowed.
 *
 * <p>Values are validated at construction (FOR-15 precedent).
 *
 * @param id stable identifier used by templates to reference this exercise; required, non-blank
 * @param name human-readable exercise name; required, non-blank
 * @param movementPattern the movement pattern; required
 * @param primaryMuscles primary muscles worked; required, non-empty
 * @param equipment required equipment (home-friendly); required
 * @param instructions concise execution instructions; required, non-blank
 */
public record Exercise(
    String id,
    String name,
    MovementPattern movementPattern,
    List<String> primaryMuscles,
    Equipment equipment,
    String instructions) {

  public Exercise {
    requireText(id, "id");
    requireText(name, "name");
    requireText(instructions, "instructions");
    Objects.requireNonNull(movementPattern, "movementPattern must not be null");
    Objects.requireNonNull(equipment, "equipment must not be null");
    Objects.requireNonNull(primaryMuscles, "primaryMuscles must not be null");
    if (primaryMuscles.isEmpty()) {
      throw new IllegalArgumentException("primaryMuscles must not be empty");
    }
    // Defensive copy so the record stays immutable.
    primaryMuscles = List.copyOf(primaryMuscles);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}

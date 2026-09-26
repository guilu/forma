package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Objects;

/**
 * A reusable strength workout template (FOR-25): a {@link WorkoutType} and an ordered list of
 * {@link StrengthWorkoutItem}s.
 *
 * <p>This is the reusable <em>template</em> — it holds exercises, sets and reps but deliberately no
 * {@code date} or completion {@code status}. Scheduling a session and marking it completed are
 * separate concerns (FOR-26/FOR-27), so those fields from docs/domain-model.md's "StrengthWorkout"
 * belong to a scheduled instance, not here (spec FOR-25 Open Questions).
 *
 * <p>Framework-free (ADR-001). Referential integrity (each {@code exerciseId} existing in the
 * FOR-24 catalog) is enforced where templates are built ({@link WorkoutTemplateCatalog}), not by
 * this type.
 *
 * @param workoutType the kind of workout; required
 * @param items ordered exercise entries; required, non-empty, with unique {@code order} values
 */
public record StrengthWorkoutTemplate(WorkoutType workoutType, List<StrengthWorkoutItem> items) {

  public StrengthWorkoutTemplate {
    Objects.requireNonNull(workoutType, "workoutType must not be null");
    Objects.requireNonNull(items, "items must not be null");
    if (items.isEmpty()) {
      throw new IllegalArgumentException("a workout template must have at least one item");
    }
    long distinctOrders = items.stream().map(StrengthWorkoutItem::order).distinct().count();
    if (distinctOrders != items.size()) {
      throw new IllegalArgumentException("item order values must be unique within a template");
    }
    // Design D6 (training-progression-and-logging): the per-serie log keys each row by
    // exerciseId, not by order — reordering a template must never silently reassign another
    // exercise's history. That invariant only holds if exerciseId is unique to begin with.
    long distinctExerciseIds =
        items.stream().map(StrengthWorkoutItem::exerciseId).distinct().count();
    if (distinctExerciseIds != items.size()) {
      throw new IllegalArgumentException("item exerciseId values must be unique within a template");
    }
    items = List.copyOf(items);
  }

  /**
   * Whether this template prescribes a set numbered {@code setNumber} (1-based) for {@code
   * exerciseId} — used by the training-set-log (design D6) to reject a write for a serie the
   * current template does not have, without the log needing its own copy of "how many sets".
   */
  public boolean hasSet(String exerciseId, int setNumber) {
    return items.stream()
        .filter(item -> item.exerciseId().equals(exerciseId))
        .anyMatch(item -> setNumber >= 1 && setNumber <= item.sets());
  }
}

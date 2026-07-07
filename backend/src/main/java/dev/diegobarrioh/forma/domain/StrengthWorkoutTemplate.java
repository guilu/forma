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
    items = List.copyOf(items);
  }
}

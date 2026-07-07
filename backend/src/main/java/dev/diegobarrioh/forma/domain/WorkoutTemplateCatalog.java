package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The initial strength workout templates (FOR-25): Push, Pull, and Legs &amp; core, built from
 * FOR-24 catalog exercises.
 *
 * <p>Defined in code with moderate volume suitable for someone also running — no periodization
 * (spec FOR-25). Consistent with the FOR-23/FOR-24 in-code approach: no persistence/migration is
 * introduced. Every referenced {@code exerciseId} is checked against {@link ExerciseCatalog} at
 * class initialization, so a typo fails fast rather than shipping a dangling reference.
 */
public final class WorkoutTemplateCatalog {

  // Moderate defaults: 3 sets, hypertrophy rep range, ~2 reps in reserve.
  private static final int SETS = 3;
  private static final int RIR = 2;
  private static final int COMPOUND_REST_SECONDS = 90;
  private static final int CORE_REST_SECONDS = 45;

  private static final List<StrengthWorkoutTemplate> TEMPLATES =
      List.of(
          new StrengthWorkoutTemplate(
              WorkoutType.PUSH,
              List.of(
                  compound("push-up", 1),
                  compound("dumbbell-shoulder-press", 2),
                  compound("bench-dip", 3))),
          new StrengthWorkoutTemplate(
              WorkoutType.PULL,
              List.of(
                  compound("pull-up", 1),
                  compound("dumbbell-row", 2),
                  compound("band-face-pull", 3))),
          new StrengthWorkoutTemplate(
              WorkoutType.LEGS,
              List.of(
                  compound("goblet-squat", 1),
                  compound("dumbbell-rdl", 2),
                  compound("reverse-lunge", 3),
                  core("dead-bug", 4))));

  private WorkoutTemplateCatalog() {}

  /** All workout templates (immutable). */
  public static List<StrengthWorkoutTemplate> templates() {
    return TEMPLATES;
  }

  /** Finds a template by its workout type. */
  public static Optional<StrengthWorkoutTemplate> findByType(WorkoutType type) {
    return TEMPLATES.stream().filter(template -> template.workoutType() == type).findFirst();
  }

  private static StrengthWorkoutItem compound(String exerciseId, int order) {
    return item(exerciseId, order, 8, 12, COMPOUND_REST_SECONDS);
  }

  private static StrengthWorkoutItem core(String exerciseId, int order) {
    return item(exerciseId, order, 10, 15, CORE_REST_SECONDS);
  }

  private static StrengthWorkoutItem item(
      String exerciseId, int order, int repsMin, int repsMax, int restSeconds) {
    // Fail fast if a template references an exercise that is not in the catalog (FOR-24).
    if (ExerciseCatalog.findById(exerciseId).isEmpty()) {
      throw new IllegalStateException("unknown catalog exerciseId: " + exerciseId);
    }
    return new StrengthWorkoutItem(exerciseId, order, SETS, repsMin, repsMax, restSeconds, RIR);
  }
}

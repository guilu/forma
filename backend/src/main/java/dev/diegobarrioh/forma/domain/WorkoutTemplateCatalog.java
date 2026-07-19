package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The strength workout templates (FOR-25, rebuilt to Diego's real plan by FOR-154): Push, Pull, and
 * Legs &amp; core, built from FOR-24 catalog exercises.
 *
 * <p>Consistent with the FOR-23/FOR-24 in-code approach: no persistence/migration is introduced.
 * Every referenced {@code exerciseId} is checked against {@link ExerciseCatalog} at class
 * initialization, so a typo fails fast rather than shipping a dangling reference.
 *
 * <p><b>FOR-154 (sheet <em>Fuerza</em> of {@code docs/fitness_os.xlsm}):</b> each template now has
 * 5 exercises with per-exercise sets/reps/RIR/rest instead of the original 3-exercise/4-exercise
 * uniform-scheme blocks. Every value below is transcribed directly from the sheet; nothing is
 * fabricated. AMRAP items (Flexiones, Dominadas) and the Plancha timed hold use {@link RepScheme}
 * (FOR-154) instead of a fixed rep range.
 *
 * <p><b>Documented assumption (per-side reps):</b> Zancadas' "10–12/pierna" is stored as a plain
 * {@link RepScheme#RANGE} of 10–12 — the "/pierna" (per-leg) qualifier is not modeled as a separate
 * domain field (spec FOR-154 Open Questions leaves this undecided); it is documented here in code
 * and the {@code reverse-lunge} catalog instructions already say "alterna piernas".
 */
public final class WorkoutTemplateCatalog {

  private static final List<StrengthWorkoutTemplate> TEMPLATES =
      List.of(
          new StrengthWorkoutTemplate(
              WorkoutType.PUSH,
              List.of(
                  StrengthWorkoutItem.range("dumbbell-bench-press", 1, 4, 8, 12, 90, 2),
                  StrengthWorkoutItem.range("dumbbell-shoulder-press", 2, 3, 8, 10, 90, 2),
                  StrengthWorkoutItem.amrap("push-up", 3, 3, 60, 1),
                  StrengthWorkoutItem.range("lateral-raise", 4, 3, 12, 20, 45, 2),
                  StrengthWorkoutItem.timeHold("plank", 5, 3, 45, 75, 45, 2))),
          new StrengthWorkoutTemplate(
              WorkoutType.PULL,
              List.of(
                  StrengthWorkoutItem.amrap("pull-up", 1, 4, 120, 1),
                  StrengthWorkoutItem.range("dumbbell-row", 2, 4, 8, 12, 90, 2),
                  StrengthWorkoutItem.range("band-face-pull", 3, 3, 15, 25, 45, 2),
                  StrengthWorkoutItem.range("biceps-curl", 4, 3, 10, 15, 60, 2),
                  StrengthWorkoutItem.range("rear-delt-fly", 5, 3, 12, 20, 45, 2))),
          new StrengthWorkoutTemplate(
              WorkoutType.LEGS,
              List.of(
                  StrengthWorkoutItem.range("goblet-squat", 1, 4, 10, 15, 90, 2),
                  StrengthWorkoutItem.range("dumbbell-rdl", 2, 4, 8, 12, 90, 2),
                  // Zancadas: "10-12/pierna" — see class javadoc, per-side note not modeled.
                  StrengthWorkoutItem.range("reverse-lunge", 3, 3, 10, 12, 90, 2),
                  StrengthWorkoutItem.range("calf-raise", 4, 4, 15, 25, 45, 1),
                  StrengthWorkoutItem.range("dead-bug", 5, 3, 10, 15, 45, 2))));

  static {
    // Fail fast if a template references an exercise that is not in the catalog (FOR-24).
    for (StrengthWorkoutTemplate template : TEMPLATES) {
      for (StrengthWorkoutItem item : template.items()) {
        if (ExerciseCatalog.findById(item.exerciseId()).isEmpty()) {
          throw new IllegalStateException("unknown catalog exerciseId: " + item.exerciseId());
        }
      }
    }
  }

  private WorkoutTemplateCatalog() {}

  /** All workout templates (immutable). */
  public static List<StrengthWorkoutTemplate> templates() {
    return TEMPLATES;
  }

  /** Finds a template by its workout type. */
  public static Optional<StrengthWorkoutTemplate> findByType(WorkoutType type) {
    return TEMPLATES.stream().filter(template -> template.workoutType() == type).findFirst();
  }
}

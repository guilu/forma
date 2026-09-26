package dev.diegobarrioh.forma.domain;

/**
 * One recorded set of a strength exercise (training-progression-and-logging slice B, design D6):
 * weight, repetitions and completion for a single {@code (exerciseId, setNumber)} pair within a
 * session's week.
 *
 * <p>Framework-free (ADR-001). {@code weightKg}/{@code reps} are independently optional — spec
 * "Registro parcial": a user who logs only the weight (e.g. an AMRAP set to bodyweight) must not
 * have the write rejected for missing reps, so each is validated on its own rather than requiring
 * both or neither.
 *
 * @param exerciseId stable id of a FOR-24 catalog exercise, matching a {@link
 *     StrengthWorkoutItem#exerciseId()} in the session's template; required, non-blank
 * @param setNumber 1-based position within the exercise's prescribed sets; must be >= 1
 * @param weightKg logged weight in kilograms, or {@code null} if not recorded; must be >= 0 when
 *     present
 * @param reps logged repetitions, or {@code null} if not recorded; must be >= 0 when present
 * @param done whether the set was marked complete
 */
public record LoggedSet(String exerciseId, int setNumber, Double weightKg, Integer reps, boolean done) {

  public LoggedSet {
    if (exerciseId == null || exerciseId.isBlank()) {
      throw new IllegalArgumentException("exerciseId must not be blank");
    }
    if (setNumber < 1) {
      throw new IllegalArgumentException("setNumber must be >= 1, was: " + setNumber);
    }
    if (weightKg != null && weightKg < 0) {
      throw new IllegalArgumentException("weightKg must be >= 0 when present, was: " + weightKg);
    }
    if (reps != null && reps < 0) {
      throw new IllegalArgumentException("reps must be >= 0 when present, was: " + reps);
    }
  }
}

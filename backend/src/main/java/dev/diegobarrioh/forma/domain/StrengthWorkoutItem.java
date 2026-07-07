package dev.diegobarrioh.forma.domain;

/**
 * One exercise entry within a {@link StrengthWorkoutTemplate} (FOR-25), per docs/domain-model.md's
 * "StrengthWorkoutItem".
 *
 * <p>Framework-free (ADR-001). References a catalog {@link Exercise} by its stable {@link
 * #exerciseId} (FOR-24) rather than embedding the exercise. Effort is expressed as reps-in-reserve
 * ({@link #rir}). Values are validated at construction.
 *
 * @param exerciseId stable id of a FOR-24 catalog exercise; required, non-blank
 * @param order 1-based position within the workout; must be >= 1
 * @param sets number of sets; must be >= 1
 * @param repsMin lower bound of the rep range; must be >= 1
 * @param repsMax upper bound of the rep range; must be >= repsMin
 * @param restSeconds rest between sets in seconds; must be >= 0
 * @param rir target reps in reserve; must be >= 0
 */
public record StrengthWorkoutItem(
    String exerciseId, int order, int sets, int repsMin, int repsMax, int restSeconds, int rir) {

  public StrengthWorkoutItem {
    if (exerciseId == null || exerciseId.isBlank()) {
      throw new IllegalArgumentException("exerciseId must not be blank");
    }
    if (order < 1) {
      throw new IllegalArgumentException("order must be >= 1, was: " + order);
    }
    if (sets < 1) {
      throw new IllegalArgumentException("sets must be >= 1, was: " + sets);
    }
    if (repsMin < 1) {
      throw new IllegalArgumentException("repsMin must be >= 1, was: " + repsMin);
    }
    if (repsMax < repsMin) {
      throw new IllegalArgumentException(
          "repsMax must be >= repsMin (" + repsMin + "), was: " + repsMax);
    }
    if (restSeconds < 0) {
      throw new IllegalArgumentException("restSeconds must be >= 0, was: " + restSeconds);
    }
    if (rir < 0) {
      throw new IllegalArgumentException("rir must be >= 0, was: " + rir);
    }
  }
}

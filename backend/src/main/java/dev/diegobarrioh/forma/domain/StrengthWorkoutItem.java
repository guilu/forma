package dev.diegobarrioh.forma.domain;

import java.util.Objects;

/**
 * One exercise entry within a {@link StrengthWorkoutTemplate} (FOR-25), per docs/domain-model.md's
 * "StrengthWorkoutItem".
 *
 * <p>Framework-free (ADR-001). References a catalog {@link Exercise} by its stable {@link
 * #exerciseId} (FOR-24) rather than embedding the exercise. Effort is expressed as reps-in-reserve
 * ({@link #rir}). Values are validated at construction.
 *
 * <p><b>Rep-scheme extension (FOR-154):</b> the original design only supported a fixed rep range.
 * Diego's real plan (sheet <em>Fuerza</em>) also needs AMRAP (train-to-failure, no rep ceiling) and
 * timed holds (seconds, not reps) — see {@link RepScheme}. Rather than overloading {@code
 * repsMin}/{@code repsMax} to mean different things per scheme, each scheme uses its own optional
 * fields and the ones that do not apply are {@code null}; the compact constructor enforces exactly
 * which fields are required per {@link #repScheme}. The {@link #range}, {@link #amrap} and {@link
 * #timeHold} factories are the intended construction path so callers never have to pass explicit
 * {@code null}s themselves.
 *
 * @param exerciseId stable id of a FOR-24 catalog exercise; required, non-blank
 * @param order 1-based position within the workout; must be >= 1
 * @param sets number of sets; must be >= 1
 * @param repScheme how the target repetitions are prescribed; required
 * @param repsMin lower bound of the rep range; required and >= 1 for {@link RepScheme#RANGE}, must
 *     be {@code null} otherwise
 * @param repsMax upper bound of the rep range; required and >= repsMin for {@link RepScheme#RANGE},
 *     must be {@code null} otherwise
 * @param durationSecondsMin lower bound of the timed hold in seconds; required and >= 1 for {@link
 *     RepScheme#TIME}, must be {@code null} otherwise
 * @param durationSecondsMax upper bound of the timed hold in seconds; required and >=
 *     durationSecondsMin for {@link RepScheme#TIME}, must be {@code null} otherwise
 * @param restSeconds rest between sets in seconds; must be >= 0
 * @param rir target reps in reserve; must be >= 0
 */
public record StrengthWorkoutItem(
    String exerciseId,
    int order,
    int sets,
    RepScheme repScheme,
    Integer repsMin,
    Integer repsMax,
    Integer durationSecondsMin,
    Integer durationSecondsMax,
    int restSeconds,
    int rir) {

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
    if (restSeconds < 0) {
      throw new IllegalArgumentException("restSeconds must be >= 0, was: " + restSeconds);
    }
    if (rir < 0) {
      throw new IllegalArgumentException("rir must be >= 0, was: " + rir);
    }
    Objects.requireNonNull(repScheme, "repScheme must not be null");
    switch (repScheme) {
      case RANGE -> {
        if (repsMin == null || repsMin < 1) {
          throw new IllegalArgumentException("repsMin must be >= 1 for RANGE, was: " + repsMin);
        }
        if (repsMax == null || repsMax < repsMin) {
          throw new IllegalArgumentException(
              "repsMax must be >= repsMin (" + repsMin + ") for RANGE, was: " + repsMax);
        }
        requireNoDuration(durationSecondsMin, durationSecondsMax, repScheme);
      }
      case AMRAP -> {
        requireNoReps(repsMin, repsMax, repScheme);
        requireNoDuration(durationSecondsMin, durationSecondsMax, repScheme);
      }
      case TIME -> {
        requireNoReps(repsMin, repsMax, repScheme);
        if (durationSecondsMin == null || durationSecondsMin < 1) {
          throw new IllegalArgumentException(
              "durationSecondsMin must be >= 1 for TIME, was: " + durationSecondsMin);
        }
        if (durationSecondsMax == null || durationSecondsMax < durationSecondsMin) {
          throw new IllegalArgumentException(
              "durationSecondsMax must be >= durationSecondsMin ("
                  + durationSecondsMin
                  + ") for TIME, was: "
                  + durationSecondsMax);
        }
      }
    }
  }

  private static void requireNoReps(Integer repsMin, Integer repsMax, RepScheme repScheme) {
    if (repsMin != null || repsMax != null) {
      throw new IllegalArgumentException(
          "repsMin/repsMax must be null for " + repScheme + ", was: " + repsMin + "/" + repsMax);
    }
  }

  private static void requireNoDuration(
      Integer durationSecondsMin, Integer durationSecondsMax, RepScheme repScheme) {
    if (durationSecondsMin != null || durationSecondsMax != null) {
      throw new IllegalArgumentException(
          "durationSecondsMin/durationSecondsMax must be null for "
              + repScheme
              + ", was: "
              + durationSecondsMin
              + "/"
              + durationSecondsMax);
    }
  }

  /** Builds a {@link RepScheme#RANGE} item: a fixed rep range. */
  public static StrengthWorkoutItem range(
      String exerciseId, int order, int sets, int repsMin, int repsMax, int restSeconds, int rir) {
    return new StrengthWorkoutItem(
        exerciseId, order, sets, RepScheme.RANGE, repsMin, repsMax, null, null, restSeconds, rir);
  }

  /** Builds a {@link RepScheme#AMRAP} item: as many reps as possible, no rep ceiling. */
  public static StrengthWorkoutItem amrap(
      String exerciseId, int order, int sets, int restSeconds, int rir) {
    return new StrengthWorkoutItem(
        exerciseId, order, sets, RepScheme.AMRAP, null, null, null, null, restSeconds, rir);
  }

  /** Builds a {@link RepScheme#TIME} item: a timed hold in seconds, not a rep count. */
  public static StrengthWorkoutItem timeHold(
      String exerciseId,
      int order,
      int sets,
      int durationSecondsMin,
      int durationSecondsMax,
      int restSeconds,
      int rir) {
    return new StrengthWorkoutItem(
        exerciseId,
        order,
        sets,
        RepScheme.TIME,
        null,
        null,
        durationSecondsMin,
        durationSecondsMax,
        restSeconds,
        rir);
  }
}

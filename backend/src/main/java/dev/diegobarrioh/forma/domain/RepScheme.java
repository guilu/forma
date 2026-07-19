package dev.diegobarrioh.forma.domain;

/**
 * How a {@link StrengthWorkoutItem}'s target repetitions are prescribed (FOR-154, sheet
 * <em>Fuerza</em> of {@code docs/fitness_os.xlsm}).
 *
 * <p>Introduced because the original fixed rep-range shape (FOR-25) cannot represent Diego's real
 * plan: some exercises are trained to failure ({@link #AMRAP}, e.g. Flexiones, Dominadas) and one
 * is a timed hold ({@link #TIME}, e.g. Plancha) rather than a rep count at all.
 */
public enum RepScheme {
  /** Fixed rep range: {@code repsMin}/{@code repsMax} are set, duration bounds are not. */
  RANGE,
  /** As Many Reps As Possible: neither rep bounds nor duration bounds are set. */
  AMRAP,
  /** Timed hold: {@code durationSecondsMin}/{@code durationSecondsMax} are set, not rep bounds. */
  TIME
}

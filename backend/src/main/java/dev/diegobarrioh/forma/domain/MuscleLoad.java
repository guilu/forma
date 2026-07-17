package dev.diegobarrioh.forma.domain;

/**
 * Load level a muscle receives within a strength session (FOR-136), derived from how many of the
 * session's exercises list that muscle in {@link Exercise#primaryMuscles()}.
 *
 * <p><b>Resolved Open Question (spec FOR-136 Data Model Notes):</b> frequency-only thresholds — 2
 * or more exercises hitting the muscle map to {@link #HIGH}; exactly 1 maps to {@link #MEDIUM} (the
 * base level for an included muscle). Movement pattern or sets/reps weighting is explicitly out of
 * scope for this MVP slice (spec Open Questions: "default frequency-only for MVP").
 *
 * <p>{@link #LOW} is part of the documented vocabulary (spec Functional Requirements:
 * "HIGH/MEDIUM/LOW") but is never produced by this pure frequency-count rule: a muscle only appears
 * in a {@link dev.diegobarrioh.forma.application.MuscleWorkedMap} once at least one exercise hits
 * it, and the lowest possible frequency (1) already maps to {@link #MEDIUM}. It is kept as a
 * reserved value for a future weighting refinement rather than removed, since the API contract
 * (spec api.md) names it explicitly — this is a documented gap, not a fabricated one.
 */
public enum MuscleLoad {
  LOW,
  MEDIUM,
  HIGH;

  /**
   * Derives the load level from how many of a session's exercises hit the muscle.
   *
   * @param exerciseCount number of exercises in the session whose {@code primaryMuscles} include
   *     the muscle; must be &gt;= 1 (a muscle absent from every exercise is never aggregated)
   */
  public static MuscleLoad fromFrequency(int exerciseCount) {
    if (exerciseCount < 1) {
      throw new IllegalArgumentException("exerciseCount must be >= 1, was: " + exerciseCount);
    }
    return exerciseCount >= 2 ? HIGH : MEDIUM;
  }
}

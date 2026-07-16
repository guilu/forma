package dev.diegobarrioh.forma.domain;

/**
 * One category's planned-vs-completed count over the FOR-129 adherence window, with the derived
 * completion rate.
 *
 * <p>Framework-free (ADR-001), pure and deterministic — the counting itself happens in the
 * application layer by reusing existing repositories/services (spec FOR-129: "no duplicated
 * counting/policy"), so this type only derives {@code rate} from already-counted {@code planned}/
 * {@code completed}. That keeps the divide-by-zero guard and the capping rule in exactly one place.
 *
 * <p><b>Resolved Open Questions (spec FOR-129):</b>
 *
 * <ul>
 *   <li>{@code rate} is {@code null} when {@code planned} is 0 — never a division by zero, and
 *       never a misleading 0% when nothing was even planned (spec Edge Cases: "Zero planned in a
 *       category ... not an error").
 *   <li>{@code rate} is capped at {@code 1.0} when {@code completed} exceeds {@code planned} (e.g.
 *       extra logged measurements beyond the expected cadence, spec Edge Cases). A UI
 *       adherence/progress indicator reads more sensibly bounded at 100% than at e.g. 150%. {@code
 *       planned}/{@code completed} themselves are never capped or altered — only the derived {@code
 *       rate} is, so the raw counts stay fully auditable (spec NFR "Explainability").
 * </ul>
 *
 * @param category the tracked category
 * @param planned the planned/expected count over the window; never negative
 * @param completed the actual count over the window; never negative, may exceed {@code planned}
 * @param rate {@code completed / planned} capped at {@code 1.0}, or {@code null} when {@code
 *     planned} is 0
 */
public record CategoryAdherence(
    AdherenceCategory category, int planned, int completed, Double rate) {

  public CategoryAdherence {
    if (planned < 0) {
      throw new IllegalArgumentException("planned must not be negative, was: " + planned);
    }
    if (completed < 0) {
      throw new IllegalArgumentException("completed must not be negative, was: " + completed);
    }
  }

  /**
   * Builds a {@link CategoryAdherence}, deriving {@code rate} from {@code planned}/{@code
   * completed}.
   */
  public static CategoryAdherence of(AdherenceCategory category, int planned, int completed) {
    return new CategoryAdherence(category, planned, completed, rate(planned, completed));
  }

  private static Double rate(int planned, int completed) {
    if (planned == 0) {
      return null;
    }
    return Math.min((double) completed / planned, 1.0);
  }
}

package dev.diegobarrioh.forma.domain;

/**
 * Completion status of a planned training session (FOR-27).
 *
 * <p>Generic across running and strength sessions. Extends docs/domain-model.md's PLANNED/COMPLETED
 * with {@link #SKIPPED}. Closed set; new values can be added later without breaking the contract.
 */
public enum SessionStatus {
  PLANNED,
  COMPLETED,
  SKIPPED
}

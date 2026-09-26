package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Whether an account has said yes to the plan it was given, and when.
 *
 * <p>Deliberately NOT a field on {@code UserProfile}. A profile says what somebody IS — height,
 * sex, objectives; this says what somebody DID, once, with a date on it. Folding an event into the
 * record that describes a person is how a profile ends up as a bag of unrelated booleans.
 */
public interface PlanAcceptanceRepository {

  /** Whether this account has accepted its plan. */
  boolean accepted(UUID userId);

  /**
   * Records the acceptance. Repeated calls keep the first instant: it happened when it happened.
   */
  void markAccepted(UUID userId, Instant at);

  /**
   * The instant this account's current plan cycle started, or empty if it never accepted one. This
   * is the single fact the current training week is derived from (no persisted week counter).
   *
   * <p>Answers the restarted cycle when one exists (design D5 of training-progression-and-logging)
   * — {@link #restartCycle} moves this answer without moving {@link #markAccepted}'s instant.
   */
  Optional<Instant> planStartedAt(UUID userId);

  /**
   * Reanchors this account's plan cycle to {@code at}, so the next {@link #planStartedAt} read
   * starts counting weeks from here (design D5). Deliberately separate from {@link #markAccepted}:
   * that method's insert-if-absent semantics exist to keep the original acceptance instant from
   * moving, and a restart is not a second acceptance.
   */
  void restartCycle(UUID userId, Instant at);
}

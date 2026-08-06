package dev.diegobarrioh.forma.application;

import java.time.Instant;
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
}

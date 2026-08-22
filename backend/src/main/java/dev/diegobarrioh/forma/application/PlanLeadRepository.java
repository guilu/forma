package dev.diegobarrioh.forma.application;

import java.time.Instant;

/**
 * Where finished funnels are kept.
 *
 * <p>Two operations and no reader: nothing in the application shows a lead to anybody yet. The
 * screen that lists them, and whatever eventually turns one into a plan, arrive with their own
 * stories — adding a `findAll` now would be an abstraction with no consumer (AGENTS.md).
 *
 * <p>{@link #deleteOlderThan} is not housekeeping. The privacy notice declares that a lead is kept
 * for twelve months, and a declared retention period with no code behind it is a promise the
 * product cannot keep.
 */
public interface PlanLeadRepository {

  /** Records a finished funnel. Returns the id it was stored under. */
  void save(PlanLead lead, Instant at);

  /**
   * Deletes every lead older than the given instant, and says how many went. Idempotent by nature:
   * a second run over the same window finds nothing left to do.
   */
  int deleteOlderThan(Instant cutoff);
}

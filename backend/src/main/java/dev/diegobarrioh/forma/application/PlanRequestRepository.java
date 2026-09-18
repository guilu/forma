package dev.diegobarrioh.forma.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Where {@link PlanRequest} rows are kept (migration V63, ADR-015).
 *
 * <p>Deliberately minimal for this slice: an insert and a read, enough to prove the schema round-
 * trips real domain objects through both engines. {@code PlanRequestService}'s capture flow, the
 * dispatcher's claim-by-{@code UPDATE}, and the ingest's status transitions are ADR-015 slices 2
 * and 6 — each needs its own method here (a conditional update for the claim, a query for the
 * dispatcher's and sweeper's scans, one for "the caller's current open request") and none of them
 * has a consumer yet. Adding them now would be exactly the unconsumed abstraction AGENTS.md warns
 * against.
 */
public interface PlanRequestRepository {

  /** Inserts a new request row. The row's {@code id} is the caller's to generate. */
  void insert(PlanRequest request);

  /** Finds a request by id, scoped to no particular user — cross-user isolation is slice 2's. */
  Optional<PlanRequest> findById(UUID id);
}

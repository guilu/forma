package dev.diegobarrioh.forma.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Where {@link PlanRequest} rows are kept (migration V63, ADR-015).
 *
 * <p>Slice 1 shipped only an insert and a read, enough to prove the schema round-trips real domain
 * objects through both engines. This slice (ADR-015 slice 2, capture) adds exactly one more method,
 * {@link #findOpenByUser(UUID)}: {@code PlanRequestService#request} needs it to enforce the
 * one-open-request rule as a clean {@code ConflictException} instead of letting the {@code
 * ux_plan_request_user_open} constraint violation leak as a 500, and {@code GET
 * /api/v1/plan-requests/current} needs the identical read to show the caller their open request.
 * One method, two call sites — the same shape {@code BodyMeasurementService#list} reuses a single
 * repository read for more than one use case.
 *
 * <p>The dispatcher's claim-by-{@code UPDATE} and the sweeper's status-scan queries remain unadded:
 * they are ADR-015 slice 6's, and neither has a consumer yet. Adding them now would be exactly the
 * unconsumed abstraction AGENTS.md warns against.
 */
public interface PlanRequestRepository {

  /** Inserts a new request row. The row's {@code id} is the caller's to generate. */
  void insert(PlanRequest request);

  /** Finds a request by id, scoped to no particular user — cross-user isolation is slice 2's. */
  Optional<PlanRequest> findById(UUID id);

  /**
   * The user's currently open request — {@code status} {@code PENDING} or {@code GENERATING},
   * equivalently {@code open_marker IS NOT NULL} — if they have one.
   *
   * <p>At most one row can ever match, enforced by {@code ux_plan_request_user_open} (ADR-015
   * decision 4); this method reads that same invariant rather than re-deriving it, so a caller
   * never has to reason about "what if there were two".
   *
   * @param userId the account to check
   * @return the open request, or empty when the account has none
   */
  Optional<PlanRequest> findOpenByUser(UUID userId);

  /**
   * Marks every request of this user's that points at this plan as {@code DELETED} (migration V65),
   * clearing {@code nutrition_plan_id} in the same write — called by {@code
   * NutritionPlanService#delete} before the plan itself is removed, so the FK's default {@code
   * RESTRICT} never sees a row still pointing at the plan being deleted.
   *
   * <p>Scoped to {@code userId} as well as {@code planId} for the same reason every other write in
   * this codebase is user-scoped: a plan id alone is not proof of ownership.
   *
   * @return how many rows were marked — 0 is an ordinary answer, not a fault: most plans have no
   *     request at all (created by hand, or a request whose plan was replaced some other way)
   */
  int markDeletedByPlan(UUID userId, UUID planId);
}

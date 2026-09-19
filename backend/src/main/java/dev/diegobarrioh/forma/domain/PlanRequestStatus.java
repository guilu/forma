package dev.diegobarrioh.forma.domain;

/**
 * Where a {@code plan_request} sits in its life (ADR-015 decision 2, migration V63; {@code DELETED}
 * added by migration V65).
 *
 * <p>{@code PENDING} moves to {@code GENERATING} when a dispatcher claims the row, and from there
 * to the two terminal states {@code READY} (a {@code nutrition_plan} was written, in the same
 * transaction) or {@code FAILED}. A retry is always a new row — this codebase does not resurrect a
 * terminal request, the same posture {@code V61__plan_lead.sql} takes toward a second funnel
 * submission ("la segunda petición es un hecho distinto de la primera").
 *
 * <p>{@code DELETED} is a fifth, terminal status reached only from {@code READY}, only when {@code
 * NutritionPlanService#delete} removes the {@code nutrition_plan} the request points at (migration
 * V65's header comment has the full reasoning: {@code ON DELETE SET NULL} would violate {@code
 * chk_plan_request_ready_has_plan}, and {@code CASCADE} would destroy the record that the plan was
 * ever requested). A {@code DELETED} row is what survives: the request as history, with its {@code
 * nutrition_plan_id} cleared because the plan it names no longer exists.
 */
public enum PlanRequestStatus {
  PENDING,
  GENERATING,
  READY,
  FAILED,
  DELETED;

  /**
   * The {@code open_marker} value this status implies: {@code '1'} while a request is still open
   * ({@link #PENDING} or {@link #GENERATING}), else null. Mirrors {@link PlanStatus#marker()} —
   * same nullable-sentinel pattern, same reason: {@code UNIQUE(user_id, open_marker)} admits any
   * number of closed requests and at most one open one, portably, because H2 cannot parse a partial
   * index. {@link #DELETED} is terminal like {@link #READY} and {@link #FAILED}, so it carries no
   * marker either.
   */
  public String openMarker() {
    return (this == PENDING || this == GENERATING) ? "1" : null;
  }
}

package dev.diegobarrioh.forma.domain;

/**
 * Where a {@code plan_request} sits in its life (ADR-015 decision 2, migration V63).
 *
 * <p>{@code PENDING} moves to {@code GENERATING} when a dispatcher claims the row, and from there
 * to the two terminal states {@code READY} (a {@code nutrition_plan} was written, in the same
 * transaction) or {@code FAILED}. A retry is always a new row — this codebase does not resurrect a
 * terminal request, the same posture {@code V61__plan_lead.sql} takes toward a second funnel
 * submission ("la segunda petición es un hecho distinto de la primera").
 */
public enum PlanRequestStatus {
  PENDING,
  GENERATING,
  READY,
  FAILED;

  /**
   * The {@code open_marker} value this status implies: {@code '1'} while a request is still open
   * ({@link #PENDING} or {@link #GENERATING}), else null. Mirrors {@link PlanStatus#marker()} —
   * same nullable-sentinel pattern, same reason: {@code UNIQUE(user_id, open_marker)} admits any
   * number of closed requests and at most one open one, portably, because H2 cannot parse a partial
   * index.
   */
  public String openMarker() {
    return (this == PENDING || this == GENERATING) ? "1" : null;
  }
}

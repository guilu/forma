package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PlanOrigin;

/**
 * How a plan came to exist (V53) — audit, never the plan itself.
 *
 * <p>{@link #metadata} is a JSON document held as text: the model that wrote the plan, the catalog
 * version it read, the constraints it was given. It is stored whole and read whole; nothing queries
 * inside it, which is why it is text rather than the JSONB H2 does not have (ADR-011). The day
 * somebody filters on it is the day it stops being a blob and becomes columns.
 *
 * <p>It must never carry the plan. The plan is rows.
 */
public record PlanGeneration(PlanOrigin by, String prompt, String metadata) {

  private static final PlanGeneration BY_HAND = new PlanGeneration(PlanOrigin.HUMAN, null, null);

  /** Somebody wrote it, so there is no prompt and no model to record. */
  public static PlanGeneration byHand() {
    return BY_HAND;
  }
}

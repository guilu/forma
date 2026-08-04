package dev.diegobarrioh.forma.domain;

/**
 * Who wrote a nutrition plan (V53).
 *
 * <p>Worth recording because the two are read differently: a plan somebody typed is a decision, and
 * a plan a model produced is a proposal whose totals have to be checked against what it claimed
 * (source document, section 11). The prompt and generation metadata beside it are audit of the
 * second case and are null for the first.
 */
public enum PlanOrigin {
  HUMAN,
  AI
}

package dev.diegobarrioh.forma.domain;

/**
 * The user's preferred weight unit (FOR-107, spec FOR-58's Ajustes mockup).
 *
 * <p>{@link #KG} is the only supported value for the MVP (metric-only, per FOR-58's Data Model
 * Notes), but this stays a real, persisted preference — not a hardcoded constant — so FOR-119 can
 * add a unit selector later without a data-model rewrite.
 */
public enum WeightUnit {
  KG
}

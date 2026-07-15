package dev.diegobarrioh.forma.domain;

/**
 * The user's preferred energy unit (FOR-107, spec FOR-58's Ajustes mockup).
 *
 * <p>{@link #KCAL} is the only supported value for the MVP (metric-only), but this stays a real,
 * persisted preference so FOR-119 can add a unit selector later without a data-model rewrite.
 */
public enum EnergyUnit {
  KCAL
}

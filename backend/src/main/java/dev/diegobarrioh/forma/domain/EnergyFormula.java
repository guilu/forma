package dev.diegobarrioh.forma.domain;

/**
 * How the basal requirement is worked out.
 *
 * <p>One value today, and an enum anyway, because the funnel shows it in a dropdown: a screen that
 * offers a choice of one is either a lie or a promise. Making it a closed classification means the
 * second one — Harris-Benedict, or Katch-McArdle for anybody who knows their body fat — is a new
 * constant and a formula, not a refactor of everything that reads it.
 */
public enum EnergyFormula {
  MIFFLIN_ST_JEOR
}

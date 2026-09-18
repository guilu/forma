package dev.diegobarrioh.forma.domain;

/**
 * How the plan agent handled a food the catalog it was given did not have (ADR-015 decision 10,
 * {@code docs/FORMA_Contrato_Agente_Plan.md} — "Cómo se nombra un alimento").
 *
 * <p>The contract forbids the one resolution that would be invisible: silently substituting a
 * different {@code foodId} without saying so. Every other outcome is named so the gap becomes
 * something the application can show, not prose buried in an instruction field.
 */
public enum UnmatchedResolution {
  /** Left as free text in the meal's {@code instructions}; nothing was listed for it. */
  INSTRUCTION,
  /** A different, real catalog id was used in its place — see {@code usedInstead}. */
  SUBSTITUTED,
  /** Dropped from the meal entirely. */
  OMITTED
}

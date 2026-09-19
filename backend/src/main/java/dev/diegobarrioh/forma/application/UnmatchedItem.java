package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.UnmatchedResolution;
import java.util.Objects;

/**
 * One gap between what the plan agent wanted to write and what the catalog it was given actually
 * had (ADR-015 decision 10, {@code docs/FORMA_Contrato_Agente_Plan.md} — "{@code unmatched}: que el
 * hueco no se pierda en la prosa"). An empty list of these is a normal, good answer; a plan that
 * silently substituted without recording one here is exactly what the contract forbids.
 *
 * @param wanted what the agent wanted to write, in natural language; required, non-blank
 * @param where the exact path inside {@code plan} this concerns, in the same shape {@link
 *     dev.diegobarrioh.forma.application.PlanProblem} already uses for structural errors (e.g.
 *     {@code "days[0].meals[2]"})
 * @param resolution how the gap was handled
 * @param usedInstead the {@code foodId} substituted in its place, when {@link
 *     UnmatchedResolution#SUBSTITUTED}; {@code null} otherwise
 * @param note why, in natural language
 */
public record UnmatchedItem(
    String wanted, String where, UnmatchedResolution resolution, String usedInstead, String note) {

  public UnmatchedItem {
    if (wanted == null || wanted.isBlank()) {
      throw new IllegalArgumentException("wanted must not be blank");
    }
    Objects.requireNonNull(resolution, "resolution must not be null");
  }
}

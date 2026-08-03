package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.ResolvedEquivalence;
import java.math.BigDecimal;

/**
 * Response body for a substitution (V47).
 *
 * <p>Delivery read model, distinct from the application types (ADR-005). The stored decision and
 * the computed answer travel flat and together: a client asking "what can I eat instead of rice"
 * wants the grams, and making it fetch two foods and do the arithmetic is how a second, divergent
 * copy of that arithmetic gets written.
 *
 * <p>{@code targetReferenceG} and every deviation are computed from the catalog at the moment of
 * the request. They are not stored, so two calls a month apart may legitimately disagree if
 * somebody corrected a food in between — that is the design working, not a bug.
 *
 * <p>A deviation is absent when it is the nutrient being held equal (zero by construction) or when
 * the source portion carries none of it, since a percentage of nothing is not a number.
 */
public record FoodEquivalenceResponse(
    String id,
    String sourceFoodId,
    String targetFoodId,
    String targetName,
    String basis,
    BigDecimal sourceReferenceG,
    double targetReferenceG,
    Double caloriesDeviationPct,
    Double proteinDeviationPct,
    Double carbsDeviationPct,
    Double fatDeviationPct,
    BigDecimal maxMacroDeviationPct,
    boolean exceedsTolerance,
    String notes) {

  public static FoodEquivalenceResponse from(ResolvedEquivalence resolved) {
    var equivalence = resolved.equivalence();
    var portion = resolved.portion();
    return new FoodEquivalenceResponse(
        equivalence.id().toString(),
        equivalence.sourceFoodId(),
        equivalence.targetFoodId(),
        resolved.targetName(),
        equivalence.basis().name(),
        equivalence.sourceReferenceG(),
        Math.round(portion.targetReferenceG() * 10.0) / 10.0,
        portion.caloriesDeviationPct(),
        portion.proteinDeviationPct(),
        portion.carbsDeviationPct(),
        portion.fatDeviationPct(),
        equivalence.maxMacroDeviationPct(),
        resolved.exceedsTolerance(),
        equivalence.notes());
  }
}

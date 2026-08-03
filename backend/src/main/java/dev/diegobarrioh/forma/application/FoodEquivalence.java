package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.EquivalenceBasis;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A curator's statement that one food may stand in for another, and on what grounds (V47).
 *
 * <p>Read model for a {@code food_equivalence} row, and it holds only the decision. The grams — how
 * much of the target actually replaces the portion — are not here and are not stored anywhere: they
 * are a function of {@code food_catalog}, so they are computed on read. See {@link
 * dev.diegobarrioh.forma.domain.EquivalentPortion}, and {@link ResolvedEquivalence} for the two
 * together.
 *
 * @param id ours; {@code null} before the row is written
 * @param sourceFoodId the food being replaced
 * @param targetFoodId the food replacing it. Directional: the opposite advice is a separate row and
 *     is never implied by this one
 * @param basis which nutrient the swap holds equal — the whole point of the equivalence
 * @param sourceReferenceG the portion of the source to talk about. The one number here a person
 *     actually picks: "let us discuss this in portions of 100 g" is editorial, not derived
 * @param maxMacroDeviationPct how far the other macros may drift before it is worth mentioning, or
 *     {@code null} when nobody has said. Never blocks anything — the swap delivers the nutrient it
 *     promised, and this only describes what it costs elsewhere
 * @param notes free text for whoever writes the advice
 * @param enabled whether the substitution is still offered. Retiring one has to be possible without
 *     deleting it
 */
public record FoodEquivalence(
    UUID id,
    String sourceFoodId,
    String targetFoodId,
    EquivalenceBasis basis,
    BigDecimal sourceReferenceG,
    BigDecimal maxMacroDeviationPct,
    String notes,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {

  /** This statement with an id, ready to be written. */
  public FoodEquivalence identifiedBy(UUID newId) {
    return new FoodEquivalence(
        newId,
        sourceFoodId,
        targetFoodId,
        basis,
        sourceReferenceG,
        maxMacroDeviationPct,
        notes,
        enabled,
        createdAt,
        updatedAt);
  }
}

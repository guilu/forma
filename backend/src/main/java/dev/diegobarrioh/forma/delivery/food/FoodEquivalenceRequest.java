package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodEquivalence;
import dev.diegobarrioh.forma.domain.EquivalenceBasis;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body accepted when stating that one food may stand in for another (V47, admin only).
 *
 * <p>Carries no grams and no ratio. Those are what the equivalence works out to, not what anybody
 * declares, and accepting them would offer a number the server has to ignore.
 *
 * @param basis required: an equivalence with no stated criterion is not an equivalence, it is two
 *     foods in a row
 * @param sourceReferenceG the portion to talk about; the one figure here a person actually picks
 * @param maxMacroDeviationPct optional, and informative only — it never refuses a substitution
 */
public record FoodEquivalenceRequest(
    @NotBlank @Size(max = 64) String sourceFoodId,
    @NotBlank @Size(max = 64) String targetFoodId,
    @NotNull EquivalenceBasis basis,
    @NotNull @DecimalMin("0.1") BigDecimal sourceReferenceG,
    @DecimalMin("0.1") BigDecimal maxMacroDeviationPct,
    String notes) {

  /** Maps the request onto the application's own type; the id is the service's to mint. */
  public FoodEquivalence toFoodEquivalence() {
    return new FoodEquivalence(
        null,
        sourceFoodId,
        targetFoodId,
        basis,
        sourceReferenceG,
        maxMacroDeviationPct,
        notes,
        true,
        null,
        null);
  }
}

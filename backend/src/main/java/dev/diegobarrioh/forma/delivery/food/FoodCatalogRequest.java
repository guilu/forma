package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.CatalogFood;
import dev.diegobarrioh.forma.domain.Preparation;
import dev.diegobarrioh.forma.domain.PrimaryMacro;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body accepted by the catalog maintenance endpoints (FOR-190, admin only).
 *
 * <p>Macros are per 100 g, matching the column headers of the Macros sheet this catalog was seeded
 * from and the stored rows. {@code servingSizeG} is the suggested portion, which is what the ration
 * columns of that sheet are computed from.
 *
 * <p>The optional key nutrients stay optional: {@code null} means "not known", never zero. The
 * catalog is full of foods whose fibre or sodium nobody has looked up, and recording a 0 for those
 * would be inventing a fact (FOR-134).
 */
public record FoodCatalogRequest(
    /*
     * Constrained to a slug because it is a stable, human-readable handle that shopping products
     * reference by foreign key and that appears in URLs — not a display name and never renameable.
     */
    @NotBlank @Pattern(regexp = "[a-z0-9-]{1,64}") String id,
    @NotBlank String name,
    @DecimalMin("0.1") BigDecimal servingSizeG,
    @PositiveOrZero int kcal,
    @PositiveOrZero BigDecimal proteinG,
    @PositiveOrZero BigDecimal carbsG,
    @PositiveOrZero BigDecimal fatG,
    @PositiveOrZero BigDecimal fiberG,
    @PositiveOrZero BigDecimal sugarsG,
    @PositiveOrZero BigDecimal sodiumMg,
    @PositiveOrZero BigDecimal saturatedFatG,
    @Size(max = 32) String foodGroupId,
    PrimaryMacro primaryMacro,
    Preparation preparation) {

  /** Maps the request onto the application's own type. */
  public CatalogFood toCatalogFood() {
    return new CatalogFood(
        id,
        name,
        servingSizeG,
        kcal,
        proteinG,
        carbsG,
        fatG,
        fiberG,
        sugarsG,
        sodiumMg,
        saturatedFatG,
        foodGroupId,
        primaryMacro,
        preparation);
  }
}

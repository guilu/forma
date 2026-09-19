package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import java.util.List;
import java.util.Objects;

/**
 * One food the plan agent is allowed to name, exactly as {@code ImportCatalogResponse.Food} ({@code
 * delivery/plan/}) renders it for a human pasting into a prompt — but built from FORMA's own
 * application/domain types ({@link FoodItem}, {@link FoodServing}) rather than that delivery-layer
 * DTO, because {@link PlanGenerationGateway} is an {@code application/} port and cannot depend on
 * {@code delivery/} without inverting ADR-001's layering (the same reason {@link
 * PlanToleranceAuditor}'s javadoc gives for living beside {@link NutritionPlan} instead of in
 * {@code domain/}).
 *
 * @param food the food's id, name and per-100g macros
 * @param preparation whether those macros describe it {@code CRUDO}, {@code COCINADO} or {@code
 *     TAL_CUAL} (V51); {@code null} when nobody has decided, which is different from "no aplica"
 * @param servings its named portions, each usable as a plan line's {@code servingId}
 */
public record PlanCatalogEntry(FoodItem food, String preparation, List<FoodServing> servings) {

  public PlanCatalogEntry {
    Objects.requireNonNull(food, "food must not be null");
    servings = servings == null ? List.of() : List.copyOf(servings);
  }
}

package dev.diegobarrioh.forma.application;

import java.util.Optional;

/**
 * Finding one recipe by id, worked out to its per-serving totals (V52).
 *
 * <p>Narrow on purpose, in the same spirit as {@link ServingLookup} over {@link
 * FoodServingRepository} and {@link dev.diegobarrioh.forma.domain.FoodLookup}: {@link
 * PlanToleranceAuditor} needs to know what one serving of a dish comes to and has no business being
 * able to create, update or delete recipes. {@link RecipeService} already computes exactly this
 * shape ({@link ResolvedRecipe#perServing()}); a caller adapts it here rather than the auditor
 * depending on the service directly, so the auditor keeps working against a recipe that has not
 * been saved yet either (slice 6's pre-write path).
 */
@FunctionalInterface
public interface RecipeLookup {

  /** The recipe with that id, resolved to its totals, or empty when nobody wrote it. */
  Optional<ResolvedRecipe> findById(String recipeId);
}

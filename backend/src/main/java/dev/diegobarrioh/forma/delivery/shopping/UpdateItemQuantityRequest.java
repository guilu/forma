package dev.diegobarrioh.forma.delivery.shopping;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/v1/shopping/list/items/{id}} (FOR-109): edits an item's
 * quantity; the backend recalculates {@code estimatedCostEur} from the product's current stored
 * price.
 *
 * <p>{@code @Min(1)} mirrors {@link dev.diegobarrioh.forma.domain.ShoppingListItem}'s own
 * invariant, validated at the API boundary (ADR-005) so an invalid quantity fails with {@code
 * VALIDATION_ERROR} (400) rather than reaching the domain constructor.
 *
 * @param quantity required new quantity; must be &gt;= 1
 */
public record UpdateItemQuantityRequest(@NotNull @Min(1) Integer quantity) {}

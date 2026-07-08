package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;

/**
 * One item in a weekly {@link ShoppingList} (FOR-37), per docs/domain-model.md's "ShoppingItem".
 *
 * <p>Framework-free (ADR-001). References a {@link ShoppingProduct} by its id (FOR-35/FOR-36)
 * rather than embedding it. {@code quantity} is flexible (units or package counts). {@code
 * estimatedCostEur} is stored here; FOR-38 may alternatively derive it from the product price ×
 * quantity. Being a record it is immutable — {@link #withChecked(boolean)} returns a copy with a
 * new checked state.
 *
 * @param productId stable id of a FOR-36 shopping product; required, non-blank
 * @param quantity number of units/packages; must be >= 1
 * @param estimatedCostEur estimated line cost in euros; required, non-negative
 * @param checked whether the item has been picked up
 */
public record ShoppingListItem(
    String productId, int quantity, BigDecimal estimatedCostEur, boolean checked) {

  public ShoppingListItem {
    if (productId == null || productId.isBlank()) {
      throw new IllegalArgumentException("productId must not be blank");
    }
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be >= 1, was: " + quantity);
    }
    if (estimatedCostEur == null) {
      throw new IllegalArgumentException("estimatedCostEur must not be null");
    }
    if (estimatedCostEur.signum() < 0) {
      throw new IllegalArgumentException("estimatedCostEur must be >= 0, was: " + estimatedCostEur);
    }
  }

  /** Returns a copy of this item with the given checked state. */
  public ShoppingListItem withChecked(boolean value) {
    return new ShoppingListItem(productId, quantity, estimatedCostEur, value);
  }
}

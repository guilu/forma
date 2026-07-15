package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingListItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for reading the weekly shopping list and persisting item checked state (FOR-39), plus the
 * FOR-109 write commands: regenerating the list and editing an item's quantity. Owned by the
 * application side; adapters implement it (ADR-001).
 */
public interface ShoppingListRepository {

  /** The current active weekly list with its items, if one exists. */
  Optional<ActiveShoppingList> findActive();

  /** Sets an item's checked state; empty if no item has the given id. */
  Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked);

  /**
   * Replaces the active list's items and stamps {@code generatedAt} (FOR-109); empty if there is no
   * active list to regenerate.
   */
  Optional<ActiveShoppingList> regenerate(List<ShoppingListItem> items, Instant generatedAt);

  /**
   * Updates an item's quantity and recalculated cost (FOR-109); empty if no item has the given id.
   */
  Optional<StoredShoppingListItem> updateQuantity(
      String itemId, int quantity, BigDecimal estimatedCostEur);

  /** Finds a single stored item by id, e.g. to resolve its product before a quantity edit. */
  Optional<StoredShoppingListItem> findItem(String itemId);
}

package dev.diegobarrioh.forma.application;

import java.util.Optional;

/**
 * Port for reading the weekly shopping list and persisting item checked state (FOR-39). Owned by
 * the application side; adapters implement it (ADR-001).
 */
public interface ShoppingListRepository {

  /** The current active weekly list with its items, if one exists. */
  Optional<ActiveShoppingList> findActive();

  /** Sets an item's checked state; empty if no item has the given id. */
  Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked);
}

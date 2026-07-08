package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingListItem;

/**
 * A persisted {@link ShoppingListItem} together with its id (FOR-39). The domain item has no
 * identity; persistence assigns an id so the checklist can toggle a specific item.
 *
 * @param id the stored item's id
 * @param item the item data
 */
public record StoredShoppingListItem(String id, ShoppingListItem item) {}

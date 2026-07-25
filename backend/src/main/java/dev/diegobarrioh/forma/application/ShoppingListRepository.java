package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingListItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for reading the weekly shopping list and persisting item checked state (FOR-39), plus the
 * FOR-109 write commands: regenerating the list and editing an item's quantity. Owned by the
 * application side; adapters implement it (ADR-001).
 *
 * <p>{@code userId} is a real account id (FOR-145c "gap table" closure, migration V33) — {@code
 * shopping_lists.user_id UUID}, FK-referencing {@code users(id)}. Before this slice the table had
 * NO owner-scoping at all. {@code shopping_list_items} (a child of {@code shopping_lists}) has no
 * {@code user_id} column of its own — every item operation is scoped through its owning list's
 * {@code user_id} (design's "child tables scoped via parent join" default).
 */
public interface ShoppingListRepository {

  /** {@code userId}'s current active weekly list with its items, if one exists. */
  Optional<ActiveShoppingList> findActive(UUID userId);

  /**
   * Sets an item's checked state, scoped to {@code userId}'s own lists; empty if no such item
   * exists for that owner.
   */
  Optional<StoredShoppingListItem> setChecked(UUID userId, String itemId, boolean checked);

  /**
   * Replaces {@code userId}'s active list's items and stamps {@code generatedAt} (FOR-109); empty
   * if that owner has no active list to regenerate.
   */
  Optional<ActiveShoppingList> regenerate(
      UUID userId, List<ShoppingListItem> items, Instant generatedAt);

  /**
   * Updates an item's quantity and recalculated cost (FOR-109), scoped to {@code userId}'s own
   * lists; empty if no such item exists for that owner.
   */
  Optional<StoredShoppingListItem> updateQuantity(
      UUID userId, String itemId, int quantity, BigDecimal estimatedCostEur);

  /**
   * Finds a single stored item by id, scoped to {@code userId}'s own lists, e.g. to resolve its
   * product before a quantity edit.
   */
  Optional<StoredShoppingListItem> findItem(UUID userId, String itemId);
}

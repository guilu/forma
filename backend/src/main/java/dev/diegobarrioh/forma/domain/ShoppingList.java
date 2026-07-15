package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A weekly shopping list (FOR-37), per docs/domain-model.md.
 *
 * <p>Framework-free (ADR-001). Keyed by {@link #weekStartDate}, it groups {@link ShoppingListItem}s
 * that reference FOR-36 products by id. An empty list (no items) is valid — a fresh weekly list.
 * Budgeting is FOR-38; a checklist UI is FOR-39.
 *
 * @param weekStartDate the Monday (or chosen start) of the list's week; required
 * @param status the list status; required
 * @param items the list's items; required, may be empty
 * @param notes optional free-text note
 * @param generatedAt when this list was generated/created (FOR-108); required. Persistence
 *     backfills a migration-time value for lists that predate this field so it is never absent.
 */
public record ShoppingList(
    LocalDate weekStartDate,
    ShoppingListStatus status,
    List<ShoppingListItem> items,
    String notes,
    Instant generatedAt) {

  public ShoppingList {
    Objects.requireNonNull(weekStartDate, "weekStartDate must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(items, "items must not be null");
    Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    items = List.copyOf(items);
  }
}

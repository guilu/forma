package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ActiveShoppingList;
import dev.diegobarrioh.forma.application.ShoppingListRepository;
import dev.diegobarrioh.forma.application.StoredShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter reading the weekly shopping list and persisting item checked state (FOR-39), plus
 * the FOR-109 write commands: rebuilding the list's items on regenerate and editing an item's
 * quantity.
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). The active list is the single row
 * with {@code status = 'ACTIVE'}; its items are read separately and combined. Reads the FOR-108
 * {@code unit}/{@code servings}/{@code generated_at} columns added by migration V9.
 */
@Repository
public class JdbcShoppingListRepository implements ShoppingListRepository {

  private static final String ITEM_COLUMNS =
      "id, product_id, quantity, estimated_cost_eur, checked, unit, servings";

  private static final RowMapper<StoredShoppingListItem> ITEM_MAPPER =
      (rs, rowNum) ->
          new StoredShoppingListItem(
              rs.getString("id"),
              new ShoppingListItem(
                  rs.getString("product_id"),
                  rs.getInt("quantity"),
                  rs.getBigDecimal("estimated_cost_eur"),
                  rs.getBoolean("checked"),
                  ShoppingUnit.valueOf(rs.getString("unit")),
                  readNullableInt(rs, "servings")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcShoppingListRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<ActiveShoppingList> findActive() {
    Optional<ActiveList> activeList = findActiveListRow();
    if (activeList.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(toActiveShoppingList(activeList.get()));
  }

  @Override
  public Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked) {
    UUID itemUuid = UUID.fromString(itemId);
    int updated =
        jdbcTemplate.update(
            "UPDATE shopping_list_items SET checked = ? WHERE id = ?", checked, itemUuid);
    if (updated == 0) {
      return Optional.empty();
    }
    return findItemRow(itemUuid);
  }

  /**
   * Replaces the active list's items and stamps {@code generatedAt} (FOR-109): deletes the existing
   * rows and inserts fresh ones with newly generated ids, matching the "replaces items" requirement
   * (spec.md) rather than trying to diff/merge with the previous list.
   */
  @Override
  public Optional<ActiveShoppingList> regenerate(
      List<ShoppingListItem> items, Instant generatedAt) {
    Optional<ActiveList> activeList = findActiveListRow();
    if (activeList.isEmpty()) {
      return Optional.empty();
    }
    UUID listId = UUID.fromString(activeList.get().id());
    jdbcTemplate.update("DELETE FROM shopping_list_items WHERE shopping_list_id = ?", listId);
    for (ShoppingListItem item : items) {
      jdbcTemplate.update(
          "INSERT INTO shopping_list_items"
              + " (id, shopping_list_id, product_id, quantity, estimated_cost_eur, checked, unit,"
              + " servings)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
          UUID.randomUUID(),
          listId,
          item.productId(),
          item.quantity(),
          item.estimatedCostEur(),
          item.checked(),
          item.unit().name(),
          item.servings());
    }
    jdbcTemplate.update(
        "UPDATE shopping_lists SET generated_at = ? WHERE id = ?",
        toOffsetDateTime(generatedAt),
        listId);
    return findActive();
  }

  @Override
  public Optional<StoredShoppingListItem> updateQuantity(
      String itemId, int quantity, BigDecimal estimatedCostEur) {
    UUID itemUuid = UUID.fromString(itemId);
    int updated =
        jdbcTemplate.update(
            "UPDATE shopping_list_items SET quantity = ?, estimated_cost_eur = ? WHERE id = ?",
            quantity,
            estimatedCostEur,
            itemUuid);
    if (updated == 0) {
      return Optional.empty();
    }
    return findItemRow(itemUuid);
  }

  @Override
  public Optional<StoredShoppingListItem> findItem(String itemId) {
    return findItemRow(UUID.fromString(itemId));
  }

  private Optional<ActiveList> findActiveListRow() {
    List<ActiveList> lists =
        jdbcTemplate.query(
            "SELECT id, week_start_date, status, notes, generated_at FROM shopping_lists"
                + " WHERE status = 'ACTIVE' ORDER BY week_start_date DESC",
            (rs, rowNum) ->
                new ActiveList(
                    rs.getString("id"),
                    rs.getObject("week_start_date", LocalDate.class),
                    ShoppingListStatus.valueOf(rs.getString("status")),
                    rs.getString("notes"),
                    rs.getObject("generated_at", OffsetDateTime.class)));
    return lists.isEmpty() ? Optional.empty() : Optional.of(lists.get(0));
  }

  private ActiveShoppingList toActiveShoppingList(ActiveList list) {
    List<StoredShoppingListItem> items =
        jdbcTemplate.query(
            "SELECT "
                + ITEM_COLUMNS
                + " FROM shopping_list_items"
                + " WHERE shopping_list_id = ? ORDER BY id",
            ITEM_MAPPER,
            UUID.fromString(list.id()));
    return new ActiveShoppingList(
        list.id(),
        list.weekStartDate(),
        list.status(),
        list.notes(),
        items,
        list.generatedAt().toInstant());
  }

  private Optional<StoredShoppingListItem> findItemRow(UUID itemUuid) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              "SELECT " + ITEM_COLUMNS + " FROM shopping_list_items WHERE id = ?",
              ITEM_MAPPER,
              itemUuid));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Integer readNullableInt(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  /** Row of {@code shopping_lists} without its items. */
  private record ActiveList(
      String id,
      LocalDate weekStartDate,
      ShoppingListStatus status,
      String notes,
      OffsetDateTime generatedAt) {}
}

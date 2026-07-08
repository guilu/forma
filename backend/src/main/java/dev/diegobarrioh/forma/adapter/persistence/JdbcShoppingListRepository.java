package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ActiveShoppingList;
import dev.diegobarrioh.forma.application.ShoppingListRepository;
import dev.diegobarrioh.forma.application.StoredShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter reading the weekly shopping list and persisting item checked state (FOR-39).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). The active list is the single row
 * with {@code status = 'ACTIVE'}; its items are read separately and combined.
 */
@Repository
public class JdbcShoppingListRepository implements ShoppingListRepository {

  private static final RowMapper<StoredShoppingListItem> ITEM_MAPPER =
      (rs, rowNum) ->
          new StoredShoppingListItem(
              rs.getString("id"),
              new ShoppingListItem(
                  rs.getString("product_id"),
                  rs.getInt("quantity"),
                  rs.getBigDecimal("estimated_cost_eur"),
                  rs.getBoolean("checked")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcShoppingListRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<ActiveShoppingList> findActive() {
    List<ActiveList> lists =
        jdbcTemplate.query(
            "SELECT id, week_start_date, status, notes FROM shopping_lists"
                + " WHERE status = 'ACTIVE' ORDER BY week_start_date DESC",
            (rs, rowNum) ->
                new ActiveList(
                    rs.getString("id"),
                    rs.getObject("week_start_date", LocalDate.class),
                    ShoppingListStatus.valueOf(rs.getString("status")),
                    rs.getString("notes")));
    if (lists.isEmpty()) {
      return Optional.empty();
    }
    ActiveList list = lists.get(0);
    List<StoredShoppingListItem> items =
        jdbcTemplate.query(
            "SELECT id, product_id, quantity, estimated_cost_eur, checked FROM shopping_list_items"
                + " WHERE shopping_list_id = ? ORDER BY id",
            ITEM_MAPPER,
            list.id());
    return Optional.of(
        new ActiveShoppingList(
            list.id(), list.weekStartDate(), list.status(), list.notes(), items));
  }

  @Override
  public Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked) {
    int updated =
        jdbcTemplate.update(
            "UPDATE shopping_list_items SET checked = ? WHERE id = ?", checked, itemId);
    if (updated == 0) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              "SELECT id, product_id, quantity, estimated_cost_eur, checked FROM"
                  + " shopping_list_items WHERE id = ?",
              ITEM_MAPPER,
              itemId));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  /** Row of {@code shopping_lists} without its items. */
  private record ActiveList(
      String id, LocalDate weekStartDate, ShoppingListStatus status, String notes) {}
}

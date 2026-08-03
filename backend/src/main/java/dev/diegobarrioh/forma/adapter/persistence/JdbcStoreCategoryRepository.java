package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.StoreCategory;
import dev.diegobarrioh.forma.application.StoreCategoryRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code store_category} (V46). Plain JDBC via {@link JdbcTemplate} (no ORM, like
 * FOR-16). Single table; the hierarchy is a column, not a join.
 */
@Repository
public class JdbcStoreCategoryRepository implements StoreCategoryRepository {

  private static final String COLUMNS =
      "id, store_id, parent_id, external_id, name, slug, level, sort_order, enabled";

  private static final RowMapper<StoreCategory> ROW_MAPPER =
      (rs, rowNum) ->
          new StoreCategory(
              rs.getString("id"),
              rs.getString("store_id"),
              rs.getString("parent_id"),
              rs.getString("external_id"),
              rs.getString("name"),
              rs.getString("slug"),
              rs.getInt("level"),
              rs.getInt("sort_order"),
              rs.getBoolean("enabled"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcStoreCategoryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoreCategory> findByStore(String storeId, boolean includeRetired) {
    // Ordered by level first so parents always precede their children, which is what makes the
    // result safe to insert elsewhere and simple to render as a tree in one pass.
    String sql =
        "SELECT "
            + COLUMNS
            + " FROM store_category WHERE store_id = ?"
            + (includeRetired ? "" : " AND enabled = TRUE")
            + " ORDER BY level, sort_order, name";
    return jdbcTemplate.query(sql, ROW_MAPPER, storeId);
  }

  @Override
  public void save(StoreCategory category) {
    // Update-then-insert rather than a MERGE: H2 and PostgreSQL spell upserts differently, and the
    // portable subset of both is two statements (ADR-011 records the same constraint elsewhere).
    int updated =
        jdbcTemplate.update(
            "UPDATE store_category SET store_id = ?, parent_id = ?, external_id = ?, name = ?,"
                + " slug = ?, level = ?, sort_order = ?, enabled = ? WHERE id = ?",
            category.storeId(),
            category.parentId(),
            category.externalId(),
            category.name(),
            category.slug(),
            category.level(),
            category.sortOrder(),
            category.enabled(),
            category.id());
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO store_category (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          category.id(),
          category.storeId(),
          category.parentId(),
          category.externalId(),
          category.name(),
          category.slug(),
          category.level(),
          category.sortOrder(),
          category.enabled());
    }
  }

  @Override
  public void retire(String id) {
    jdbcTemplate.update("UPDATE store_category SET enabled = FALSE WHERE id = ?", id);
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.Store;
import dev.diegobarrioh.forma.application.StoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code store} (V45). Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16).
 * Single table, no joins.
 */
@Repository
public class JdbcStoreRepository implements StoreRepository {

  private static final String COLUMNS = "id, name, logo_url, website, sort_order, enabled";

  private static final RowMapper<Store> ROW_MAPPER =
      (rs, rowNum) ->
          new Store(
              rs.getString("id"),
              rs.getString("name"),
              rs.getString("logo_url"),
              rs.getString("website"),
              rs.getInt("sort_order"),
              rs.getBoolean("enabled"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcStoreRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<Store> findAll() {
    // Ordered by sort_order, not by name: "Otras" belongs last however the names sort.
    return jdbcTemplate.query("SELECT " + COLUMNS + " FROM store ORDER BY sort_order", ROW_MAPPER);
  }

  @Override
  public Optional<Store> find(String id) {
    return jdbcTemplate
        .query("SELECT " + COLUMNS + " FROM store WHERE id = ?", ROW_MAPPER, id)
        .stream()
        .findFirst();
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.FoodEquivalence;
import dev.diegobarrioh.forma.application.FoodEquivalenceRepository;
import dev.diegobarrioh.forma.domain.EquivalenceBasis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code food_equivalence} (V47). Plain JDBC via {@link JdbcTemplate} (no ORM,
 * like FOR-16).
 *
 * <p>Reads the decision and nothing else. The grams a substitution works out to are computed from
 * the catalog by the domain, so there is no join to fetch them and no column to select.
 */
@Repository
public class JdbcFoodEquivalenceRepository implements FoodEquivalenceRepository {

  private static final String COLUMNS =
      "id, source_food_id, target_food_id, basis, source_reference_g, max_macro_deviation_pct,"
          + " notes, enabled, created_at, updated_at";

  private static final RowMapper<FoodEquivalence> ROW_MAPPER =
      (rs, rowNum) ->
          new FoodEquivalence(
              rs.getObject("id", UUID.class),
              rs.getString("source_food_id"),
              rs.getString("target_food_id"),
              EquivalenceBasis.valueOf(rs.getString("basis")),
              rs.getBigDecimal("source_reference_g"),
              rs.getBigDecimal("max_macro_deviation_pct"),
              rs.getString("notes"),
              rs.getBoolean("enabled"),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("updated_at").toInstant());

  private final JdbcTemplate jdbcTemplate;

  public JdbcFoodEquivalenceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<FoodEquivalence> findBySource(String sourceFoodId) {
    // Retired advice is left out here rather than filtered by every caller: a substitution nobody
    // offers any more is not an answer to "what can I eat instead".
    return jdbcTemplate.query(
        "SELECT "
            + COLUMNS
            + " FROM food_equivalence WHERE source_food_id = ? AND enabled = TRUE"
            + " ORDER BY basis, target_food_id",
        ROW_MAPPER,
        sourceFoodId);
  }

  @Override
  public Optional<FoodEquivalence> find(
      String sourceFoodId, String targetFoodId, EquivalenceBasis basis) {
    // Deliberately ignores `enabled`: the natural key is taken whether or not the advice is still
    // offered, and reporting it as free would trip the unique index instead.
    return jdbcTemplate
        .query(
            "SELECT "
                + COLUMNS
                + " FROM food_equivalence WHERE source_food_id = ? AND target_food_id = ?"
                + " AND basis = ?",
            ROW_MAPPER,
            sourceFoodId,
            targetFoodId,
            basis.name())
        .stream()
        .findFirst();
  }

  @Override
  public void insert(FoodEquivalence equivalence) {
    jdbcTemplate.update(
        "INSERT INTO food_equivalence (id, source_food_id, target_food_id, basis,"
            + " source_reference_g, max_macro_deviation_pct, notes, enabled)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        equivalence.id(),
        equivalence.sourceFoodId(),
        equivalence.targetFoodId(),
        equivalence.basis().name(),
        equivalence.sourceReferenceG(),
        equivalence.maxMacroDeviationPct(),
        equivalence.notes(),
        equivalence.enabled());
  }

  @Override
  public boolean delete(UUID id) {
    return jdbcTemplate.update("DELETE FROM food_equivalence WHERE id = ?", id) > 0;
  }
}

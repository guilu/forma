package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ImportedMeasureMarkerStore;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists imported-Withings-measure-group markers in the {@code
 * integration_measure_marker} table (FOR-132, migration V16) — entirely separate from {@code
 * body_measurements}, so {@code BodyMeasurement} stays provider-clean (spec FOR-132).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003). {@code markImported} is an
 * insert-if-absent upsert (a group is marked at most once; marking it again is a safe no-op),
 * following the same update-then-insert-on-zero-rows shape used elsewhere in this adapter package,
 * adapted for a natural three-column primary key with no separate update path (the row never
 * changes once written).
 */
@Repository
public class JdbcImportedMeasureMarkerStore implements ImportedMeasureMarkerStore {

  private static final String FIND_IMPORTED_SQL =
      "SELECT grpid FROM integration_measure_marker WHERE owner_id = ? AND provider = ?";

  private static final String INSERT_SQL =
      """
      INSERT INTO integration_measure_marker (owner_id, provider, grpid, imported_at)
      VALUES (?, ?, ?, ?)
      """;

  private static final String EXISTS_SQL =
      """
      SELECT COUNT(*) FROM integration_measure_marker
      WHERE owner_id = ? AND provider = ? AND grpid = ?
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcImportedMeasureMarkerStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Set<Long> findImportedGroupIds(String ownerId, IntegrationProvider provider) {
    List<Long> ids =
        jdbcTemplate.query(
            FIND_IMPORTED_SQL, (rs, rowNum) -> rs.getLong("grpid"), ownerId, provider.name());
    return new LinkedHashSet<>(ids);
  }

  @Override
  public void markImported(
      String ownerId, IntegrationProvider provider, long groupId, Instant importedAt) {
    Integer existing =
        jdbcTemplate.queryForObject(EXISTS_SQL, Integer.class, ownerId, provider.name(), groupId);
    if (existing != null && existing > 0) {
      return;
    }
    jdbcTemplate.update(
        INSERT_SQL,
        ownerId,
        provider.name(),
        groupId,
        OffsetDateTime.ofInstant(importedAt, ZoneOffset.UTC));
  }
}

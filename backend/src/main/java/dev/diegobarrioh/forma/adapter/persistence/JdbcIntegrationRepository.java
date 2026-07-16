package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.IntegrationRepository;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link IntegrationConnection}s to the single-row-per-(owner, provider)
 * {@code integration_connection} table (FOR-126, migration V12).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003). {@code save} is a portable
 * update-then-insert upsert, following {@link JdbcUserProfileRepository}'s pattern (works on both
 * PostgreSQL and the H2 test database) rather than {@link JdbcGoalRepository}'s generated-id
 * insert, because a connection's identity is the (owner, provider) pair itself, not a separate
 * generated id.
 *
 * <p><b>No token/secret column exists here</b> (ADR-004, spec FOR-126 boundary rule) — this is the
 * adapter later FOR-103 slices extend with encrypted token storage, without changing the {@link
 * IntegrationRepository} port.
 *
 * <p>{@code last_sync_duplicates_skipped} (migration V16, FOR-132) is additive on this same table —
 * it records the last sync's dedup count alongside the existing {@code last_sync_*} columns, not a
 * new table, mirroring how the rest of {@link SyncOutcome} is already flattened here.
 */
@Repository
public class JdbcIntegrationRepository implements IntegrationRepository {

  private static final String FIND_ALL_SQL =
      """
      SELECT provider, status, connected_at, last_sync_at,
        last_sync_result, last_sync_imported_count, last_sync_duplicates_skipped, last_sync_message
      FROM integration_connection
      WHERE owner_id = ?
      ORDER BY provider
      """;

  private static final String FIND_ONE_SQL =
      """
      SELECT provider, status, connected_at, last_sync_at,
        last_sync_result, last_sync_imported_count, last_sync_duplicates_skipped, last_sync_message
      FROM integration_connection
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String UPDATE_SQL =
      """
      UPDATE integration_connection SET
        status = ?, connected_at = ?, last_sync_at = ?,
        last_sync_result = ?, last_sync_imported_count = ?, last_sync_duplicates_skipped = ?,
        last_sync_message = ?
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO integration_connection
        (owner_id, provider, status, connected_at, last_sync_at,
         last_sync_result, last_sync_imported_count, last_sync_duplicates_skipped, last_sync_message)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final RowMapper<IntegrationConnection> ROW_MAPPER =
      (rs, rowNum) ->
          new IntegrationConnection(
              IntegrationProvider.valueOf(rs.getString("provider")),
              IntegrationStatus.valueOf(rs.getString("status")),
              toInstant(rs.getObject("connected_at", OffsetDateTime.class)),
              toInstant(rs.getObject("last_sync_at", OffsetDateTime.class)),
              toSyncOutcome(
                  rs.getString("last_sync_result"),
                  (Integer) rs.getObject("last_sync_imported_count"),
                  (Integer) rs.getObject("last_sync_duplicates_skipped"),
                  rs.getString("last_sync_message")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcIntegrationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<IntegrationConnection> findAllByOwner(String ownerId) {
    return jdbcTemplate.query(FIND_ALL_SQL, ROW_MAPPER, ownerId);
  }

  @Override
  public Optional<IntegrationConnection> findByOwnerAndProvider(
      String ownerId, IntegrationProvider provider) {
    List<IntegrationConnection> found =
        jdbcTemplate.query(FIND_ONE_SQL, ROW_MAPPER, ownerId, provider.name());
    return found.stream().findFirst();
  }

  @Override
  public IntegrationConnection save(String ownerId, IntegrationConnection connection) {
    SyncOutcome outcome = connection.lastSyncOutcome();
    String status = connection.status().name();
    OffsetDateTime connectedAt = toOffsetDateTime(connection.connectedAt());
    OffsetDateTime lastSyncAt = toOffsetDateTime(connection.lastSyncAt());
    String lastSyncResult = outcome == null ? null : outcome.result().name();
    Integer lastSyncImportedCount = outcome == null ? null : outcome.importedCount();
    Integer lastSyncDuplicatesSkipped = outcome == null ? null : outcome.duplicatesSkipped();
    String lastSyncMessage = outcome == null ? null : outcome.message();

    int updated =
        jdbcTemplate.update(
            UPDATE_SQL,
            status,
            connectedAt,
            lastSyncAt,
            lastSyncResult,
            lastSyncImportedCount,
            lastSyncDuplicatesSkipped,
            lastSyncMessage,
            ownerId,
            connection.provider().name());
    if (updated == 0) {
      jdbcTemplate.update(
          INSERT_SQL,
          ownerId,
          connection.provider().name(),
          status,
          connectedAt,
          lastSyncAt,
          lastSyncResult,
          lastSyncImportedCount,
          lastSyncDuplicatesSkipped,
          lastSyncMessage);
    }
    return connection;
  }

  private static SyncOutcome toSyncOutcome(
      String result, Integer importedCount, Integer duplicatesSkipped, String message) {
    if (result == null) {
      return null;
    }
    return new SyncOutcome(
        SyncResult.valueOf(result),
        importedCount == null ? 0 : importedCount,
        duplicatesSkipped == null ? 0 : duplicatesSkipped,
        message);
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }
}

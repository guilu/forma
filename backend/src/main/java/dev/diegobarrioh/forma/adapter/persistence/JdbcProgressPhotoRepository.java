package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import dev.diegobarrioh.forma.application.ProgressPhotoRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists progress-photo metadata to the {@code progress_photo} table (FOR-140,
 * migration V19). Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003). Only metadata is stored
 * here; the binary lives behind {@link
 * dev.diegobarrioh.forma.adapter.storage.FilesystemProgressPhotoStore}.
 *
 * <p>{@link #findById} is deliberately NOT owner-scoped at the SQL level — see the port javadoc for
 * why: the service needs to distinguish an unknown id (404) from another owner's photo (403).
 */
@Repository
public class JdbcProgressPhotoRepository implements ProgressPhotoRepository {

  private static final RowMapper<ProgressPhotoMetadata> ROW_MAPPER =
      (rs, rowNum) ->
          new ProgressPhotoMetadata(
              rs.getString("id"),
              rs.getString("owner_id"),
              rs.getString("content_type"),
              rs.getLong("size_bytes"),
              rs.getObject("created_at", OffsetDateTime.class).toInstant(),
              rs.getString("storage_ref"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcProgressPhotoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ProgressPhotoMetadata create(
      String id,
      String ownerId,
      String contentType,
      long sizeBytes,
      String storageRef,
      Instant createdAt) {
    jdbcTemplate.update(
        "INSERT INTO progress_photo (id, owner_id, content_type, size_bytes, storage_ref,"
            + " created_at) VALUES (?, ?, ?, ?, ?, ?)",
        UUID.fromString(id),
        ownerId,
        contentType,
        sizeBytes,
        storageRef,
        OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    return new ProgressPhotoMetadata(id, ownerId, contentType, sizeBytes, createdAt, storageRef);
  }

  @Override
  public List<ProgressPhotoMetadata> findAllByOwner(String ownerId) {
    return jdbcTemplate.query(
        "SELECT id, owner_id, content_type, size_bytes, storage_ref, created_at FROM"
            + " progress_photo WHERE owner_id = ? ORDER BY created_at, id",
        ROW_MAPPER,
        ownerId);
  }

  @Override
  public Optional<ProgressPhotoMetadata> findById(String id) {
    List<ProgressPhotoMetadata> found =
        jdbcTemplate.query(
            "SELECT id, owner_id, content_type, size_bytes, storage_ref, created_at FROM"
                + " progress_photo WHERE id = ?",
            ROW_MAPPER,
            UUID.fromString(id));
    return found.stream().findFirst();
  }

  @Override
  public void deleteById(String id) {
    jdbcTemplate.update("DELETE FROM progress_photo WHERE id = ?", UUID.fromString(id));
  }
}

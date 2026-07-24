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
 * <p>FOR-145b-1 (migration V27): {@link #findById} is now owner-scoped at the SQL level (WHERE id =
 * ? AND user_id = ?) -- a cross-owner id and an unknown id are now indistinguishable, so the
 * service maps both to 404 (no existence leak), replacing the pre-145b 403-vs-404 split. Legacy
 * {@code owner_id VARCHAR} column stays populated (userId.toString()) but is never read here.
 */
@Repository
public class JdbcProgressPhotoRepository implements ProgressPhotoRepository {

  private static final RowMapper<ProgressPhotoMetadata> ROW_MAPPER =
      (rs, rowNum) ->
          new ProgressPhotoMetadata(
              rs.getString("id"),
              (UUID) rs.getObject("user_id"),
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
      UUID userId,
      String contentType,
      long sizeBytes,
      String storageRef,
      Instant createdAt) {
    jdbcTemplate.update(
        "INSERT INTO progress_photo (id, owner_id, user_id, content_type, size_bytes,"
            + " storage_ref, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
        UUID.fromString(id),
        userId.toString(),
        userId,
        contentType,
        sizeBytes,
        storageRef,
        OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    return new ProgressPhotoMetadata(id, userId, contentType, sizeBytes, createdAt, storageRef);
  }

  @Override
  public List<ProgressPhotoMetadata> findAllByOwner(UUID userId) {
    return jdbcTemplate.query(
        "SELECT id, user_id, content_type, size_bytes, storage_ref, created_at FROM"
            + " progress_photo WHERE user_id = ? ORDER BY created_at, id",
        ROW_MAPPER,
        userId);
  }

  @Override
  public Optional<ProgressPhotoMetadata> findById(UUID userId, String id) {
    UUID photoId;
    try {
      photoId = UUID.fromString(id);
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    List<ProgressPhotoMetadata> found =
        jdbcTemplate.query(
            "SELECT id, user_id, content_type, size_bytes, storage_ref, created_at FROM"
                + " progress_photo WHERE id = ? AND user_id = ?",
            ROW_MAPPER,
            photoId,
            userId);
    return found.stream().findFirst();
  }

  @Override
  public void deleteById(String id) {
    jdbcTemplate.update("DELETE FROM progress_photo WHERE id = ?", UUID.fromString(id));
  }
}

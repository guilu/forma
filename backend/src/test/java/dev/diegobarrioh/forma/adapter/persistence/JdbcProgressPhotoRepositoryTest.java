package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import dev.diegobarrioh.forma.application.ProgressPhotoRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcProgressPhotoRepository} (FOR-140). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V19), like the FOR-135 {@code
 * JdbcAchievementRepositoryTest}. Covers the metadata round-trip.
 *
 * <p>FOR-145b-1 (migration V27): {@code progress_photo.user_id} FK-references {@code users(id)}, so
 * {@code OTHER_OWNER} must be a real seeded row. {@link ProgressPhotoRepository#findById} is now
 * owner-scoped at the SQL level (unlike the pre-145b unscoped contract) — a cross-owner id behaves
 * exactly like an unknown id (empty), which is the property the service relies on to return 404
 * (never 403) without an existence leak.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcProgressPhotoRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();

  @Autowired private ProgressPhotoRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM progress_photo");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "progressphotorepo-other-owner@test.local",
        "!");
  }

  /**
   * Leaves no live {@code progress_photo} rows referencing {@code OTHER_OWNER} after the last test
   * in this class runs (ADR-007 shared named in-memory H2 across the whole test run) — otherwise a
   * later test class that blanket-deletes non-placeholder {@code users} rows (e.g. {@code
   * AuthenticationFlowIntegrationTest#clearTestUsers}) would hit an FK violation.
   */
  @AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM progress_photo");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
  }

  @Test
  void findAllByOwnerIsEmptyOnACleanDatabase() {
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void createdPhotoRoundTripsThroughFindAllByOwner() {
    String id = UUID.randomUUID().toString();
    Instant createdAt = Instant.parse("2026-07-18T10:00:00Z");

    ProgressPhotoMetadata created =
        repository.create(id, OWNER, "image/jpeg", 20480L, id, createdAt);
    List<ProgressPhotoMetadata> all = repository.findAllByOwner(OWNER);

    assertThat(created.id()).isEqualTo(id);
    assertThat(all).hasSize(1);
    ProgressPhotoMetadata read = all.get(0);
    assertThat(read.ownerId()).isEqualTo(OWNER);
    assertThat(read.contentType()).isEqualTo("image/jpeg");
    assertThat(read.sizeBytes()).isEqualTo(20480L);
    assertThat(read.storageRef()).isEqualTo(id);
    assertThat(read.createdAt()).isEqualTo(createdAt);
  }

  @Test
  void findAllByOwnerNeverReturnsAnotherOwnersPhotos() {
    String id = UUID.randomUUID().toString();
    repository.create(
        id, OTHER_OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void findByIdReturnsEmptyForAnUnknownId() {
    assertThat(repository.findById(OWNER, UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void findByIdReturnsEmptyWhenThePhotoBelongsToAnotherOwner() {
    String id = UUID.randomUUID().toString();
    repository.create(
        id, OTHER_OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    Optional<ProgressPhotoMetadata> found = repository.findById(OWNER, id);

    assertThat(found).isEmpty();
  }

  @Test
  void findByIdReturnsThePhotoWhenItBelongsToTheRequestingOwner() {
    String id = UUID.randomUUID().toString();
    repository.create(
        id, OTHER_OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    Optional<ProgressPhotoMetadata> found = repository.findById(OTHER_OWNER, id);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().ownerId()).isEqualTo(OTHER_OWNER);
  }

  @Test
  void deleteByIdRemovesTheRowAndIsANoOpForAnUnknownId() {
    String id = UUID.randomUUID().toString();
    repository.create(id, OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    repository.deleteById(id);
    repository.deleteById(UUID.randomUUID().toString());

    assertThat(repository.findById(OWNER, id)).isEmpty();
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }
}

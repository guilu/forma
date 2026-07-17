package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import dev.diegobarrioh.forma.application.ProgressPhotoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcProgressPhotoRepository} (FOR-140). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V19), like the FOR-135 {@code
 * JdbcAchievementRepositoryTest}. Covers the metadata round-trip and the {@code findById} contract
 * this repository must uphold that {@code GoalRepository} does not: it returns a photo regardless
 * of owner so the service can distinguish "unknown" (404) from "someone else's" (403).
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcProgressPhotoRepositoryTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";

  @Autowired private ProgressPhotoRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM progress_photo");
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
    assertThat(repository.findById(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void findByIdReturnsAPhotoRegardlessOfOwnerSoTheServiceCanDistinguish403From404() {
    String id = UUID.randomUUID().toString();
    repository.create(
        id, OTHER_OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    Optional<ProgressPhotoMetadata> found = repository.findById(id);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().ownerId()).isEqualTo(OTHER_OWNER);
  }

  @Test
  void deleteByIdRemovesTheRowAndIsANoOpForAnUnknownId() {
    String id = UUID.randomUUID().toString();
    repository.create(id, OWNER, "image/jpeg", 100L, id, Instant.parse("2026-07-18T10:00:00Z"));

    repository.deleteById(id);
    repository.deleteById(UUID.randomUUID().toString());

    assertThat(repository.findById(id)).isEmpty();
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }
}

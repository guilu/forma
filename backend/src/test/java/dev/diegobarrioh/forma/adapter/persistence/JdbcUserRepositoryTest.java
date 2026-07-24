package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.UserRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcUserRepository} (FOR-145, migration V26, ADR-012). Runs against
 * the in-memory PostgreSQL-mode H2 with Flyway migrations applied, like {@code
 * JdbcGoalRepositoryTest}/{@code JdbcUserProfileRepositoryTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcUserRepositoryTest {

  @Autowired private UserRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTestRows() {
    // Never delete the seeded placeholder row (V26) — only rows this test class creates.
    jdbcTemplate.update("DELETE FROM users WHERE id <> ?", LegacyUserBootstrap.PLACEHOLDER_USER_ID);
  }

  @Test
  void migrationSeedsAnUnusablePlaceholderRow() {
    Optional<User> placeholder = repository.findById(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

    assertThat(placeholder).isPresent();
    assertThat(placeholder.get().email()).isEqualTo("legacy@forma.local");
    assertThat(placeholder.get().passwordHash()).isEqualTo("!");
    assertThat(placeholder.get().active()).isFalse();
  }

  @Test
  void findByEmailIsEmptyWhenNoAccountExists() {
    assertThat(repository.findByEmail("nobody@x.com")).isEmpty();
  }

  @Test
  void insertThenFindByEmailAndFindByIdRoundTrip() {
    UUID id = UUID.randomUUID();

    repository.insert(id, "a@x.com", "{argon2}somehash");

    Optional<User> byEmail = repository.findByEmail("a@x.com");
    assertThat(byEmail).isPresent();
    assertThat(byEmail.get().id()).isEqualTo(id);
    assertThat(byEmail.get().passwordHash()).isEqualTo("{argon2}somehash");
    assertThat(byEmail.get().active()).isTrue();
    assertThat(byEmail.get().lastLoginAt()).isNull();
    assertThat(byEmail.get().createdAt()).isNotNull();

    assertThat(repository.findById(id)).isPresent();
  }

  @Test
  void existsByEmailReflectsInsertedRows() {
    assertThat(repository.existsByEmail("b@x.com")).isFalse();

    repository.insert(UUID.randomUUID(), "b@x.com", "{argon2}somehash");

    assertThat(repository.existsByEmail("b@x.com")).isTrue();
  }

  @Test
  void updateLastLoginAtPersistsTheTimestamp() {
    UUID id = UUID.randomUUID();
    repository.insert(id, "c@x.com", "{argon2}somehash");
    Instant loginAt = Instant.now();

    repository.updateLastLoginAt(id, loginAt);

    Optional<User> reloaded = repository.findById(id);
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().lastLoginAt()).isNotNull();
  }

  @Test
  void findByIdOfAnUnknownAccountIsEmpty() {
    assertThat(repository.findById(UUID.randomUUID())).isEmpty();
  }
}

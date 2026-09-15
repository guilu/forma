package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void insertWithGoogleSubjectCreatesAnActiveAccountWithNoPasswordHash() {
    UUID id = UUID.randomUUID();

    repository.insertWithGoogleSubject(id, "google.only@x.com", "google-sub-1");

    Optional<User> stored = repository.findById(id);
    assertThat(stored).isPresent();
    assertThat(stored.get().passwordHash()).isNull();
    assertThat(stored.get().active()).isTrue();
    assertThat(stored.get().googleSubject()).isEqualTo("google-sub-1");
  }

  @Test
  void findByGoogleSubjectFindsAnAccountLinkedToThatSubject() {
    UUID id = UUID.randomUUID();
    repository.insertWithGoogleSubject(id, "google.find@x.com", "google-sub-2");

    Optional<User> found = repository.findByGoogleSubject("google-sub-2");

    assertThat(found).isPresent();
    assertThat(found.get().id()).isEqualTo(id);
  }

  @Test
  void findByGoogleSubjectIsEmptyWhenNoAccountIsLinked() {
    assertThat(repository.findByGoogleSubject("no-such-subject")).isEmpty();
  }

  @Test
  void linkGoogleSubjectAttachesTheSubjectWithoutTouchingThePasswordHash() {
    UUID id = UUID.randomUUID();
    repository.insert(id, "linkme@x.com", "{argon2}somehash");

    repository.linkGoogleSubject(id, "google-sub-3");

    Optional<User> reloaded = repository.findById(id);
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().googleSubject()).isEqualTo("google-sub-3");
    assertThat(reloaded.get().passwordHash()).isEqualTo("{argon2}somehash");
  }

  @Test
  void googleSubjectMustBeUniqueAcrossAccounts() {
    repository.insertWithGoogleSubject(UUID.randomUUID(), "first@x.com", "duplicate-sub");

    assertThatThrownBy(
            () ->
                repository.insertWithGoogleSubject(
                    UUID.randomUUID(), "second@x.com", "duplicate-sub"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}

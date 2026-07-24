package dev.diegobarrioh.forma.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.UserRepository;
import dev.diegobarrioh.forma.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests {@link LegacyUserBootstrap} (FOR-145, ADR-012): the seeded V26 placeholder row stays
 * unusable with no configured secret, and activates only when one is explicitly provided (never a
 * literal committed value — AGENTS.md).
 *
 * <p>The H2 test database ({@code jdbc:h2:mem:forma}) is a single named in-memory instance shared
 * by every {@code @SpringBootTest} context in this JVM (same convention as {@code
 * JdbcGoalRepositoryTest} et al.) — a distinct {@code @TestPropertySource} still gets its own
 * {@code ApplicationContext} (and therefore re-runs {@link LegacyUserBootstrap} at startup), but
 * both contexts read/write the SAME underlying {@code users} row. {@code WithAConfiguredSecret}
 * therefore restores the placeholder row after itself so it never leaks its activation into any
 * other test class that reads the same row (e.g. {@code JdbcUserRepositoryTest}).
 */
class LegacyUserBootstrapTest {

  @SpringBootTest
  @ActiveProfiles("test")
  @Nested
  class WithNoConfiguredSecret {

    @Autowired private UserRepository userRepository;

    @Test
    void placeholderRowStaysUnusable() {
      Optional<User> placeholder = userRepository.findById(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

      assertThat(placeholder).isPresent();
      assertThat(placeholder.get().active()).isFalse();
      assertThat(placeholder.get().passwordHash()).isEqualTo("!");
    }
  }

  @SpringBootTest
  @ActiveProfiles("test")
  @TestPropertySource(properties = "forma.bootstrap.legacy-user-password=Str0ngLegacyP@ssw0rd!")
  @Nested
  class WithAConfiguredSecret {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restorePlaceholderRow() {
      jdbcTemplate.update(
          "UPDATE users SET password_hash = '!', is_active = FALSE WHERE id = ?",
          LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    }

    @Test
    void activatesThePlaceholderRowWithAnArgon2idHashOfTheConfiguredPassword() {
      Optional<User> placeholder = userRepository.findById(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

      assertThat(placeholder).isPresent();
      assertThat(placeholder.get().active()).isTrue();
      assertThat(placeholder.get().passwordHash()).isNotEqualTo("!");
      assertThat(passwordEncoder.matches("Str0ngLegacyP@ssw0rd!", placeholder.get().passwordHash()))
          .isTrue();
    }
  }
}

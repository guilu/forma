package dev.diegobarrioh.forma.bootstrap;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Activates the seeded legacy placeholder account (FOR-145, ADR-012 / migration V26) at startup,
 * config-driven — never from a secret committed to a migration or to this class.
 *
 * <p>V26 seeds {@code users} with a placeholder row (id {@code
 * 00000000-0000-0000-0000-000000000000}) that is unusable by construction: {@code password_hash =
 * '!'} is never a valid encoded hash, and {@code is_active = false}. If, and only if, {@code
 * forma.bootstrap.legacy-user-password} is set (an environment-provided credential, never a literal
 * value in this codebase — AGENTS.md "Do not commit secrets"), this runner Argon2-hashes it via the
 * same {@link PasswordEncoder} used for every other account and activates the row. With no property
 * set, the row is left exactly as the migration seeded it: unusable.
 *
 * <p>This lets the single legacy pre-auth user (the app's original single-user data owner, whose
 * historical rows will be backfilled onto this same placeholder id in the following slices, 145b/
 * 145c) claim a real login without ever writing a plaintext or committed credential anywhere.
 */
@Component
public class LegacyUserBootstrap implements ApplicationRunner {

  /** Matches V26's seeded placeholder row and every v2 table's {@code PLACEHOLDER_USER_ID}. */
  public static final UUID PLACEHOLDER_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static final Logger log = LoggerFactory.getLogger(LegacyUserBootstrap.class);

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final String legacyUserPassword;

  public LegacyUserBootstrap(
      JdbcTemplate jdbcTemplate,
      PasswordEncoder passwordEncoder,
      @Value("${forma.bootstrap.legacy-user-password:}") String legacyUserPassword) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
    this.legacyUserPassword = legacyUserPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (legacyUserPassword == null || legacyUserPassword.isBlank()) {
      // No secret configured — leave the placeholder row unusable, exactly as V26 seeded it.
      return;
    }
    String hash = passwordEncoder.encode(legacyUserPassword);
    int updated =
        jdbcTemplate.update(
            "UPDATE users SET password_hash = ?, is_active = TRUE WHERE id = ?",
            hash,
            PLACEHOLDER_USER_ID);
    if (updated > 0) {
      log.info("Legacy placeholder account activated (id={})", PLACEHOLDER_USER_ID);
    }
  }
}

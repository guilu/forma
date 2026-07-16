package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.OAuthChallenge;
import dev.diegobarrioh.forma.application.OAuthStateStore;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for the short-lived, single-use OAuth state/PKCE challenge store (FOR-131, migration
 * V15, {@code integration_oauth_state} table) — the resolved "persisted, not in-memory" choice
 * documented on {@link OAuthStateStore}.
 *
 * <p>Generates {@code state} and the PKCE {@code code_verifier} as 256-bit random values, base64url
 * (no padding) encoded — well within RFC 7636's 43-128 character {@code code_verifier} length
 * requirement. {@code code_challenge} is the S256 method: {@code BASE64URL(SHA256(code_verifier))}.
 * Single-use is enforced by deleting the row on a successful {@link #consume}, not by a schema flag
 * — a replayed {@code state} finds no row and is rejected.
 */
@Repository
public class JdbcOAuthStateStore implements OAuthStateStore {

  /** How long an issued challenge remains valid before it must be rejected as expired. */
  static final Duration CHALLENGE_TTL = Duration.ofMinutes(10);

  private static final int RANDOM_BYTES = 32;

  private static final String FIND_SQL =
      """
      SELECT state, code_verifier, code_challenge, expires_at
      FROM integration_oauth_state
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String UPDATE_SQL =
      """
      UPDATE integration_oauth_state SET
        state = ?, code_verifier = ?, code_challenge = ?, expires_at = ?
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO integration_oauth_state
        (owner_id, provider, state, code_verifier, code_challenge, expires_at)
      VALUES (?, ?, ?, ?, ?, ?)
      """;

  private static final String DELETE_SQL =
      "DELETE FROM integration_oauth_state WHERE owner_id = ? AND provider = ?";

  private final JdbcTemplate jdbcTemplate;
  private final SecureRandom secureRandom = new SecureRandom();

  public JdbcOAuthStateStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public OAuthChallenge create(String ownerId, IntegrationProvider provider, Instant now) {
    String state = randomUrlSafeToken();
    String codeVerifier = randomUrlSafeToken();
    String codeChallenge = pkceS256(codeVerifier);
    Instant expiresAt = now.plus(CHALLENGE_TTL);
    OffsetDateTime expiresAtColumn = OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC);

    int updated =
        jdbcTemplate.update(
            UPDATE_SQL,
            state,
            codeVerifier,
            codeChallenge,
            expiresAtColumn,
            ownerId,
            provider.name());
    if (updated == 0) {
      jdbcTemplate.update(
          INSERT_SQL,
          ownerId,
          provider.name(),
          state,
          codeVerifier,
          codeChallenge,
          expiresAtColumn);
    }
    return new OAuthChallenge(state, codeVerifier, codeChallenge, expiresAt);
  }

  @Override
  public Optional<OAuthChallenge> consume(
      String ownerId, IntegrationProvider provider, String state, Instant now) {
    RowMapper<OAuthChallenge> rowMapper =
        (rs, rowNum) ->
            new OAuthChallenge(
                rs.getString("state"),
                rs.getString("code_verifier"),
                rs.getString("code_challenge"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant());
    Optional<OAuthChallenge> stored =
        jdbcTemplate.query(FIND_SQL, rowMapper, ownerId, provider.name()).stream().findFirst();

    if (stored.isEmpty()) {
      return Optional.empty();
    }
    OAuthChallenge challenge = stored.get();
    if (!constantTimeEquals(challenge.state(), state) || now.isAfter(challenge.expiresAt())) {
      return Optional.empty();
    }

    jdbcTemplate.update(DELETE_SQL, ownerId, provider.name());
    return Optional.of(challenge);
  }

  private String randomUrlSafeToken() {
    byte[] bytes = new byte[RANDOM_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String pkceS256(String codeVerifier) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  /** Constant-time comparison so validating {@code state} does not leak timing information. */
  private static boolean constantTimeEquals(String a, String b) {
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}

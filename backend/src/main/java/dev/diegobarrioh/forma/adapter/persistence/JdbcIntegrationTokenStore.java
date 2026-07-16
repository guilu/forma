package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.ExchangedTokens;
import dev.diegobarrioh.forma.application.IntegrationTokenStore;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists provider OAuth tokens encrypted at rest, in the single-row-per-
 * (owner, provider) {@code integration_token} table (FOR-131, migration V15) — entirely separate
 * from {@code integration_connection} (FOR-126, kept token-free).
 *
 * <p>Encryption/decryption is delegated to {@link AesGcmTokenCipher}: this class never sees a
 * "should I encrypt this" decision, it always does, and it never logs a plaintext or ciphertext
 * value (ADR-008). Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link
 * JdbcIntegrationRepository}'s update-then-insert upsert pattern.
 */
@Repository
public class JdbcIntegrationTokenStore implements IntegrationTokenStore {

  private static final String FIND_SQL =
      """
      SELECT access_token_ciphertext, access_token_nonce,
        refresh_token_ciphertext, refresh_token_nonce, access_token_expires_at
      FROM integration_token
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String UPDATE_SQL =
      """
      UPDATE integration_token SET
        access_token_ciphertext = ?, access_token_nonce = ?,
        refresh_token_ciphertext = ?, refresh_token_nonce = ?,
        access_token_expires_at = ?, updated_at = ?
      WHERE owner_id = ? AND provider = ?
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO integration_token
        (owner_id, provider, access_token_ciphertext, access_token_nonce,
         refresh_token_ciphertext, refresh_token_nonce, access_token_expires_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String DELETE_SQL =
      "DELETE FROM integration_token WHERE owner_id = ? AND provider = ?";

  private final JdbcTemplate jdbcTemplate;
  private final AesGcmTokenCipher cipher;

  public JdbcIntegrationTokenStore(JdbcTemplate jdbcTemplate, AesGcmTokenCipher cipher) {
    this.jdbcTemplate = jdbcTemplate;
    this.cipher = cipher;
  }

  @Override
  public void store(String ownerId, IntegrationProvider provider, ExchangedTokens tokens) {
    AesGcmTokenCipher.EncryptedValue access = cipher.encrypt(tokens.accessToken());
    AesGcmTokenCipher.EncryptedValue refresh = cipher.encrypt(tokens.refreshToken());
    OffsetDateTime expiresAt = toOffsetDateTime(tokens.accessTokenExpiresAt());
    OffsetDateTime updatedAt = toOffsetDateTime(Instant.now());

    int updated =
        jdbcTemplate.update(
            UPDATE_SQL,
            access.ciphertext(),
            access.nonce(),
            refresh.ciphertext(),
            refresh.nonce(),
            expiresAt,
            updatedAt,
            ownerId,
            provider.name());
    if (updated == 0) {
      jdbcTemplate.update(
          INSERT_SQL,
          ownerId,
          provider.name(),
          access.ciphertext(),
          access.nonce(),
          refresh.ciphertext(),
          refresh.nonce(),
          expiresAt,
          updatedAt);
    }
  }

  @Override
  public Optional<ExchangedTokens> find(String ownerId, IntegrationProvider provider) {
    RowMapper<ExchangedTokens> rowMapper =
        (rs, rowNum) ->
            new ExchangedTokens(
                cipher.decrypt(
                    rs.getBytes("access_token_ciphertext"), rs.getBytes("access_token_nonce")),
                cipher.decrypt(
                    rs.getBytes("refresh_token_ciphertext"), rs.getBytes("refresh_token_nonce")),
                toInstant(rs.getObject("access_token_expires_at", OffsetDateTime.class)));
    List<ExchangedTokens> found = jdbcTemplate.query(FIND_SQL, rowMapper, ownerId, provider.name());
    return found.stream().findFirst();
  }

  @Override
  public void forget(String ownerId, IntegrationProvider provider) {
    jdbcTemplate.update(DELETE_SQL, ownerId, provider.name());
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(OffsetDateTime value) {
    return value.toInstant();
  }
}

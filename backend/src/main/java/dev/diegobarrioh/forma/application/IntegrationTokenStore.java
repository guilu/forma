package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for encrypted-at-rest storage of a provider's OAuth tokens (FOR-131 spec Data Model Notes:
 * "New table (V15)... AES-256-GCM ciphertext + per-token nonce columns... Do NOT add tokens to
 * integration_connection"). Owned by the application side; the adapter implementation (persistence)
 * owns the encryption itself — callers of this port only ever see plaintext {@link ExchangedTokens}
 * in memory, never ciphertext/nonce, and the table this writes to is entirely separate from {@code
 * integration_connection} (kept token-free by {@link IntegrationRepository}).
 *
 * <p>This port is deliberately internal to {@link IntegrationService} — no controller, response DTO
 * or other application service is ever given access to it, so a stored token can never reach an API
 * response (spec FOR-131 api.md: "No endpoint ever returns a token, refresh token, code, or
 * state").
 *
 * <p>Every method is owner-scoped (ADR-002), matching {@link IntegrationRepository}.
 *
 * <p>{@code userId} is a real account id (FOR-145b-2, migration V28) — {@code integration_token}'s
 * composite primary key was rebuilt from the legacy {@code owner_id VARCHAR} column to {@code
 * user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface IntegrationTokenStore {

  /**
   * Encrypts and stores {@code tokens} for {@code userId}/{@code provider}, replacing any
   * previously stored tokens for that owner/provider (upsert — a fresh token exchange or a refresh
   * both call this).
   */
  void store(UUID userId, IntegrationProvider provider, ExchangedTokens tokens);

  /** The stored tokens for {@code userId}/{@code provider}, decrypted; empty if none are stored. */
  Optional<ExchangedTokens> find(UUID userId, IntegrationProvider provider);

  /**
   * Deletes any stored tokens for {@code userId}/{@code provider} so nothing remains at rest (spec
   * FOR-131 Edge Cases: "Disconnect while tokens exist → tokens removed"). Idempotent no-op when
   * nothing is stored (spec FOR-131 Edge Cases: "disconnect when already disconnected →
   * idempotent").
   */
  void forget(UUID userId, IntegrationProvider provider);
}

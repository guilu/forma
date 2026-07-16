package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.util.Optional;

/**
 * Port for a short-lived, single-use, expiring OAuth state/PKCE challenge store (FOR-131 spec Data
 * Model Notes: "a short-lived persisted table OR a documented in-memory store; must expire and be
 * single-use"). Owned by the application side; adapters implement it (ADR-001).
 *
 * <p><b>Resolved decision</b> (spec FOR-131 Open Questions: "OAuth state store: persisted vs
 * in-memory — pick for MVP + document"): persisted (a small table via migration V15), not
 * in-memory. FORMA already has a Postgres/Flyway baseline and every other piece of state in this
 * codebase is persisted, so a persisted store needs no new infrastructure, survives an application
 * restart between "user clicked connect" and "user finished the Withings consent screen" (a
 * realistic multi-second-to-minutes gap an in-memory store would lose), and is testable the same
 * way as everything else (H2 + Flyway, ADR-007) instead of needing a separate in-memory-store test
 * strategy.
 *
 * <p>Every method is owner-scoped (ADR-002), matching {@link IntegrationRepository}. Unlike {@link
 * IntegrationRepository}, this port's whole purpose is to carry short-lived OAuth material
 * (state/PKCE) needed to complete a token exchange — it is intentionally not held to {@link
 * IntegrationRepository}'s "no token accessor" rule, because a state/PKCE challenge is not a
 * provider access/refresh token (spec FOR-131 Data Model Notes: encryption-at-rest is required for
 * access/refresh tokens specifically, not for this table).
 */
public interface OAuthStateStore {

  /**
   * Issues a fresh, single-use challenge for {@code ownerId}/{@code provider}, expiring after a
   * short, implementation-defined window. Replaces (overwrites) any previous unconsumed challenge
   * for the same owner/provider, so re-triggering connect before finishing a previous attempt
   * simply invalidates the old attempt rather than accumulating rows.
   */
  OAuthChallenge create(String ownerId, IntegrationProvider provider, Instant now);

  /**
   * Validates and single-use-consumes the challenge matching {@code ownerId}/{@code provider}/
   * {@code state} as of {@code now}: empty when no challenge exists, the stored challenge's {@code
   * state} does not match, or it has expired (spec FOR-131 Edge Cases: "mismatched/expired/replayed
   * state → reject"). A successful consume removes the challenge so a replayed callback with the
   * same {@code state} always sees it as gone — this is what makes "single-use" true, not just
   * documented.
   */
  Optional<OAuthChallenge> consume(
      String ownerId, IntegrationProvider provider, String state, Instant now);
}

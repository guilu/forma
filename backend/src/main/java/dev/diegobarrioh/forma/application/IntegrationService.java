package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use cases for provider-neutral integration connections. FOR-126 (first implementable
 * slice of FOR-103) shipped the status read model and a mock connect/disconnect/ manual-sync shell.
 * FOR-131 (slice 2) adds a real Withings OAuth connect/callback/disconnect and encrypted token
 * refresh on top of that shell — every other registered provider still falls back to the FOR-126
 * mock (see {@link ConnectResult}), since only Withings has a registered OAuth app (spec FOR-131).
 * FOR-132 (slice 3, final) replaces {@link #sync}'s stub with a real Withings Getmeas import into
 * {@code BodyMeasurement} — refresh token if needed, fetch measure groups, map, skip
 * already-imported {@code grpid}s, save the rest, and record a real {@link SyncOutcome}. Providers
 * with no registered {@link ProviderMeasuresGateway} (Google Fit, Apple Health — out of FOR-132
 * scope) keep the FOR-126 stub/no-op sync behavior unchanged.
 *
 * <p>Real multi-user auth (FOR-145b-2, ADR-012, migration V28): every use case resolves the
 * caller's account id via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID =
 * "default-user"} constant (removed by this slice) — {@code integration_connection}, {@code
 * integration_token}, {@code integration_oauth_state} and {@code integration_measure_marker} all
 * had their composite primary keys rebuilt from the legacy {@code owner_id VARCHAR} column to
 * {@code user_id UUID}. {@link #bodyMeasurementRepository} is NOT scoped here: {@code
 * body_measurements} is a 145c "gap table" (no {@code user_id} column at all yet, see {@code
 * AdherenceService}'s documented limitation) — a real Withings sync import from a non-placeholder
 * user still writes into the same global, unscoped measurement history until 145c closes that gap.
 */
@Service
public class IntegrationService {

  /** User-readable, secret-free sync failure messages (ADR-004/ADR-008 — never leak tokens). */
  private static final String MESSAGE_NOT_CONNECTED = "El proveedor no está conectado.";

  private static final String MESSAGE_NEEDS_REAUTH =
      "Reconecta el proveedor para seguir sincronizando.";
  private static final String MESSAGE_PROVIDER_ERROR =
      "El proveedor no está disponible temporalmente, inténtalo más tarde.";

  private final IntegrationRepository repository;
  private final OAuthStateStore stateStore;
  private final IntegrationTokenStore tokenStore;
  private final ImportedMeasureMarkerStore markerStore;
  private final BodyMeasurementRepository bodyMeasurementRepository;
  private final Map<IntegrationProvider, ProviderOAuthGateway> gatewaysByProvider;
  private final Map<IntegrationProvider, ProviderMeasuresGateway> measuresGatewaysByProvider;
  private final CurrentUserProvider currentUserProvider;

  public IntegrationService(
      IntegrationRepository repository,
      OAuthStateStore stateStore,
      IntegrationTokenStore tokenStore,
      ImportedMeasureMarkerStore markerStore,
      BodyMeasurementRepository bodyMeasurementRepository,
      List<ProviderOAuthGateway> gateways,
      List<ProviderMeasuresGateway> measuresGateways,
      CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.stateStore = stateStore;
    this.tokenStore = tokenStore;
    this.markerStore = markerStore;
    this.bodyMeasurementRepository = bodyMeasurementRepository;
    this.gatewaysByProvider =
        gateways.stream()
            .collect(Collectors.toMap(ProviderOAuthGateway::provider, Function.identity()));
    this.measuresGatewaysByProvider =
        measuresGateways.stream()
            .collect(Collectors.toMap(ProviderMeasuresGateway::provider, Function.identity()));
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Per-provider connection status for every known {@link IntegrationProvider}, defaulting to
   * {@link IntegrationStatus#DISCONNECTED} for any provider never connected (spec FOR-126 Edge
   * Cases: "GET before any connection → 200 with all known providers DISCONNECTED, never 404").
   */
  public List<IntegrationConnection> status() {
    return List.of(IntegrationProvider.values()).stream().map(this::currentOrDefault).toList();
  }

  /**
   * Starts connecting {@code provider}. When a {@link ProviderOAuthGateway} is registered for it
   * (Withings, FOR-131), issues a single-use OAuth state/PKCE challenge, marks the connection
   * {@link IntegrationStatus#PENDING} (unless already {@link IntegrationStatus#CONNECTED} — see
   * {@link IntegrationConnection#awaitingCallback()}), and returns {@link
   * ConnectResult#authorizationRequired(String)}. Otherwise falls back to the FOR-126 mock
   * immediate-connect and returns {@link ConnectResult#connected(IntegrationConnection)} (spec
   * FOR-131 only implements Withings; Google Fit/Apple Health have no registered OAuth app yet —
   * documented scope decision, see {@link ConnectResult}).
   */
  public ConnectResult connect(IntegrationProvider provider) {
    Optional<ProviderOAuthGateway> gateway = gatewayFor(provider);
    if (gateway.isEmpty()) {
      IntegrationConnection connected = currentOrDefault(provider).connect(Instant.now());
      return ConnectResult.connected(
          repository.save(currentUserProvider.currentUserId(), connected));
    }

    Instant now = Instant.now();
    OAuthChallenge challenge =
        stateStore.create(currentUserProvider.currentUserId(), provider, now);
    repository.save(
        currentUserProvider.currentUserId(), currentOrDefault(provider).awaitingCallback());
    String authorizationUrl =
        gateway.get().buildAuthorizationUrl(challenge.state(), challenge.codeChallenge());
    return ConnectResult.authorizationRequired(authorizationUrl);
  }

  /**
   * Completes an OAuth round trip for {@code provider} (FOR-131): validates {@code state} against a
   * stored, unexpired, single-use challenge (spec FOR-131 Edge Cases: "mismatched/expired/replayed
   * state → reject, no connection created, no tokens stored"), exchanges {@code code} for tokens
   * via the registered {@link ProviderOAuthGateway}, stores them encrypted, and marks the
   * connection {@link IntegrationStatus#CONNECTED}.
   *
   * <p>The state challenge is consumed (removed) before the token exchange is attempted, so a
   * failed exchange cannot be retried with the same {@code code}/{@code state} — the caller must
   * restart the connect flow, matching how Withings itself treats authorization codes as
   * single-use.
   *
   * @throws ValidationException if {@code provider} has no registered OAuth gateway
   * @throws OAuthStateException if {@code state} does not match a live, unconsumed challenge
   * @throws ProviderOAuthException if the token exchange fails; the connection is left unchanged
   *     (still {@link IntegrationStatus#PENDING} or whatever it was before)
   */
  public IntegrationConnection callback(IntegrationProvider provider, String code, String state) {
    ProviderOAuthGateway gateway =
        gatewayFor(provider)
            .orElseThrow(
                () ->
                    new ValidationException("El proveedor no admite conexión OAuth: " + provider));

    Instant now = Instant.now();
    OAuthChallenge challenge =
        stateStore
            .consume(currentUserProvider.currentUserId(), provider, state, now)
            .orElseThrow(OAuthStateException::new);

    ExchangedTokens tokens = gateway.exchangeAuthorizationCode(code, challenge.codeVerifier());

    tokenStore.store(currentUserProvider.currentUserId(), provider, tokens);
    IntegrationConnection connected = currentOrDefault(provider).connect(now);
    return repository.save(currentUserProvider.currentUserId(), connected);
  }

  /**
   * Marks {@code provider} disconnected and forgets any stored tokens (spec FOR-131 Functional
   * Requirements: "Disconnect revokes/forgets the stored tokens"). Idempotent no-op on the
   * connection status when already disconnected (spec FOR-126 Edge Cases); forgetting tokens is
   * always safe to call even when none are stored.
   */
  public IntegrationConnection disconnect(IntegrationProvider provider) {
    tokenStore.forget(currentUserProvider.currentUserId(), provider);
    IntegrationConnection disconnected = currentOrDefault(provider).disconnect();
    return repository.save(currentUserProvider.currentUserId(), disconnected);
  }

  /**
   * Refreshes {@code provider}'s stored access token if it has expired as of {@code now}, using the
   * stored refresh token (spec FOR-131 Functional Requirements: "Token refresh when the access
   * token is expired"). A no-op when no tokens are stored, or when the current access token is
   * still valid. On refresh failure, marks the connection {@link IntegrationStatus#NEEDS_REAUTH}
   * instead of silently dropping the connection (spec FOR-131 Edge Cases: "Refresh failure → mark
   * connection needing re-auth; do not silently drop").
   *
   * <p>Called automatically by {@link #sync} (FOR-132) for any provider with a registered {@link
   * ProviderMeasuresGateway}, before it calls the Withings Measure API — the seam this was
   * originally added for. {@code now} is an explicit parameter (matching {@link
   * IntegrationConnection#connect(Instant)}'s style) so callers — and tests — control the instant
   * expiry is evaluated against.
   */
  public IntegrationConnection refreshTokenIfNeeded(IntegrationProvider provider, Instant now) {
    Optional<ExchangedTokens> stored =
        tokenStore.find(currentUserProvider.currentUserId(), provider);
    if (stored.isEmpty()) {
      return currentOrDefault(provider);
    }
    if (now.isBefore(stored.get().accessTokenExpiresAt())) {
      return currentOrDefault(provider);
    }

    ProviderOAuthGateway gateway =
        gatewayFor(provider)
            .orElseThrow(
                () ->
                    new ValidationException("El proveedor no admite conexión OAuth: " + provider));
    try {
      ExchangedTokens refreshed = gateway.refreshTokens(stored.get().refreshToken());
      tokenStore.store(currentUserProvider.currentUserId(), provider, refreshed);
      return currentOrDefault(provider);
    } catch (ProviderOAuthException ex) {
      return repository.save(
          currentUserProvider.currentUserId(), currentOrDefault(provider).needsReauth());
    }
  }

  /**
   * Triggers a manual sync (FOR-126, real Withings import added by FOR-132). Otherwise
   * (disconnected, still {@link IntegrationStatus#PENDING} an OAuth callback) resolves the spec's
   * open question by returning a readable {@link SyncResult#NOT_CONNECTED} outcome instead of a 409
   * (keeps the FOR-57/FOR-123 frontend error handling simple, spec FOR-126 Edge Cases) — nothing is
   * persisted, since no sync actually ran. FOR-131 tightened this from "not DISCONNECTED" to "is
   * CONNECTED" so a mid-handshake provider is never treated as sync-able.
   *
   * <p>When {@link IntegrationStatus#CONNECTED} and a {@link ProviderMeasuresGateway} is registered
   * for {@code provider} (Withings, FOR-132): refreshes the access token first (spec FOR-132 Edge
   * Cases: "Token expired → refreshed first"); if the refresh fails, records a {@link
   * SyncResult#NEEDS_REAUTH} outcome instead of attempting a sync with a stale token. Otherwise
   * fetches measure groups since the last successful sync ({@link
   * IntegrationConnection#lastSyncAt()}, incremental — spec FOR-132: "prefer incremental sync"),
   * skips groups whose {@code grpid} is already recorded in {@link ImportedMeasureMarkerStore}
   * (idempotent duplicate detection, ADR-004 — ), saves the rest via {@link
   * BodyMeasurementRepository}, marks each newly-imported {@code grpid}, and records the real
   * counts. A provider-side failure (unreachable, rate-limited, 5xx, unparseable) is caught and
   * converted to a readable {@link SyncResult#ERROR} outcome — the connection stays {@link
   * IntegrationStatus#CONNECTED} (spec FOR-132 Edge Cases: "connection not corrupted, no crash"),
   * never propagated as an exception.
   *
   * <p>Providers without a registered {@link ProviderMeasuresGateway} (Google Fit, Apple Health —
   * out of FOR-132 scope) keep the FOR-126 stub/no-op import ({@code importedCount} always {@code
   * 0}).
   */
  public IntegrationConnection sync(IntegrationProvider provider) {
    IntegrationConnection current = currentOrDefault(provider);
    if (current.status() != IntegrationStatus.CONNECTED) {
      SyncOutcome notConnected =
          new SyncOutcome(SyncResult.NOT_CONNECTED, 0, 0, MESSAGE_NOT_CONNECTED);
      return current.withSyncOutcome(current.lastSyncAt(), notConnected);
    }

    Optional<ProviderMeasuresGateway> measuresGateway = measuresGatewayFor(provider);
    if (measuresGateway.isEmpty()) {
      SyncOutcome stubOutcome = new SyncOutcome(SyncResult.OK, 0, 0, null);
      IntegrationConnection synced = current.withSyncOutcome(Instant.now(), stubOutcome);
      return repository.save(currentUserProvider.currentUserId(), synced);
    }

    IntegrationConnection refreshed = refreshTokenIfNeeded(provider, Instant.now());
    if (refreshed.status() == IntegrationStatus.NEEDS_REAUTH) {
      SyncOutcome needsReauth =
          new SyncOutcome(SyncResult.NEEDS_REAUTH, 0, 0, MESSAGE_NEEDS_REAUTH);
      return repository.save(
          currentUserProvider.currentUserId(),
          refreshed.withSyncOutcome(Instant.now(), needsReauth));
    }

    Optional<ExchangedTokens> tokens =
        tokenStore.find(currentUserProvider.currentUserId(), provider);
    if (tokens.isEmpty()) {
      // Defensive: CONNECTED with a registered OAuth gateway should always have stored tokens
      // (they are set together in #callback); treat the impossible case the same as a failed
      // refresh rather than crashing or calling the provider with no credentials.
      SyncOutcome needsReauth =
          new SyncOutcome(SyncResult.NEEDS_REAUTH, 0, 0, MESSAGE_NEEDS_REAUTH);
      return repository.save(
          currentUserProvider.currentUserId(),
          refreshed.withSyncOutcome(Instant.now(), needsReauth));
    }

    List<ImportedMeasureGroup> fetchedGroups;
    try {
      fetchedGroups =
          measuresGateway
              .get()
              .fetchMeasureGroups(tokens.get().accessToken(), refreshed.lastSyncAt());
    } catch (ProviderSyncException ex) {
      SyncOutcome errorOutcome = new SyncOutcome(SyncResult.ERROR, 0, 0, MESSAGE_PROVIDER_ERROR);
      return repository.save(
          currentUserProvider.currentUserId(),
          refreshed.withSyncOutcome(Instant.now(), errorOutcome));
    }

    Set<Long> alreadyImported =
        markerStore.findImportedGroupIds(currentUserProvider.currentUserId(), provider);
    Instant importedAt = Instant.now();
    int importedCount = 0;
    int duplicatesSkipped = 0;
    for (ImportedMeasureGroup group : fetchedGroups) {
      if (alreadyImported.contains(group.externalGroupId())) {
        duplicatesSkipped++;
        continue;
      }
      BodyMeasurement measurement = group.measurement();
      bodyMeasurementRepository.save(measurement);
      markerStore.markImported(
          currentUserProvider.currentUserId(), provider, group.externalGroupId(), importedAt);
      importedCount++;
    }

    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, importedCount, duplicatesSkipped, null);
    IntegrationConnection synced = refreshed.withSyncOutcome(importedAt, outcome);
    return repository.save(currentUserProvider.currentUserId(), synced);
  }

  private Optional<ProviderMeasuresGateway> measuresGatewayFor(IntegrationProvider provider) {
    return Optional.ofNullable(measuresGatewaysByProvider.get(provider));
  }

  private Optional<ProviderOAuthGateway> gatewayFor(IntegrationProvider provider) {
    return Optional.ofNullable(gatewaysByProvider.get(provider));
  }

  private IntegrationConnection currentOrDefault(IntegrationProvider provider) {
    return repository
        .findByOwnerAndProvider(currentUserProvider.currentUserId(), provider)
        .orElseGet(() -> IntegrationConnection.disconnectedDefault(provider));
  }
}

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link IntegrationService} (FOR-126 status/sync/mock-connect,
 * extended by FOR-131 with real Withings OAuth connect/callback/disconnect/refresh, and by FOR-132
 * with a real Withings Getmeas import into {@code BodyMeasurement}). Uses hand-rolled in-memory
 * fakes for every port (no Spring, no Mockito), matching {@code GoalServiceTest} (FOR-125,
 * ADR-007). {@link IntegrationProvider#WITHINGS} has a fake OAuth gateway and a fake measures
 * gateway registered (mirroring the real {@code WithingsOAuthAdapter}/{@code
 * WithingsMeasuresAdapter} being the only production-registered implementations); {@link
 * IntegrationProvider#GOOGLE_FIT} has neither, so it exercises the FOR-126 mock-connect + stub-sync
 * fallback unchanged by this story.
 */
class IntegrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-16T08:00:00Z");
  private static final Instant LATER = Instant.parse("2026-07-16T09:00:00Z");

  private final RecordingIntegrationRepository repository = new RecordingIntegrationRepository();
  private final FakeOAuthStateStore stateStore = new FakeOAuthStateStore();
  private final FakeIntegrationTokenStore tokenStore = new FakeIntegrationTokenStore();
  private final FakeImportedMeasureMarkerStore markerStore = new FakeImportedMeasureMarkerStore();
  private final FakeBodyMeasurementRepository bodyMeasurementRepository =
      new FakeBodyMeasurementRepository();
  private final FakeWithingsGateway withingsGateway = new FakeWithingsGateway();
  private final FakeWithingsMeasuresGateway withingsMeasuresGateway =
      new FakeWithingsMeasuresGateway();
  private final IntegrationService service =
      new IntegrationService(
          repository,
          stateStore,
          tokenStore,
          markerStore,
          bodyMeasurementRepository,
          List.of(withingsGateway),
          List.of(withingsMeasuresGateway));

  // --- status (FOR-126, unaffected by FOR-131) ---------------------------------------------

  @Test
  void statusListsEveryKnownProviderDefaultingToDisconnectedWhenNoneStored() {
    List<IntegrationConnection> status = service.status();

    assertThat(status).hasSize(IntegrationProvider.values().length);
    assertThat(status)
        .allSatisfy(c -> assertThat(c.status()).isEqualTo(IntegrationStatus.DISCONNECTED));
  }

  // --- connect: provider WITHOUT a registered gateway keeps the FOR-126 mock behavior --------

  @Test
  void connectOnAProviderWithoutARegisteredGatewayMarksItConnectedAndPersistsIt() {
    ConnectResult result = service.connect(IntegrationProvider.GOOGLE_FIT);

    assertThat(result.authorizationUrl()).isNull();
    assertThat(result.connection().status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(result.connection().connectedAt()).isNotNull();
    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.GOOGLE_FIT)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.CONNECTED);
  }

  @Test
  void connectOnAProviderWithoutARegisteredGatewayWhenAlreadyConnectedIsIdempotent() {
    ConnectResult first = service.connect(IntegrationProvider.GOOGLE_FIT);

    ConnectResult second = service.connect(IntegrationProvider.GOOGLE_FIT);

    assertThat(second.connection().status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(second.connection().connectedAt()).isEqualTo(first.connection().connectedAt());
  }

  @Test
  void connectIsOwnerScoped() {
    service.connect(IntegrationProvider.WITHINGS);

    assertThat(repository.findByOwnerAndProvider("someone-else", IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  // --- connect: WITHINGS has a registered gateway -> real OAuth round trip ------------------

  @Test
  void connectOnAProviderWithARegisteredGatewayReturnsAnAuthorizationUrlAndMarksItPending() {
    ConnectResult result = service.connect(IntegrationProvider.WITHINGS);

    assertThat(result.connection()).isNull();
    assertThat(result.authorizationUrl()).isNotBlank();
    IntegrationConnection stored =
        repository
            .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
            .orElseThrow();
    assertThat(stored.status()).isEqualTo(IntegrationStatus.PENDING);
    assertThat(stored.connectedAt()).isNull();
  }

  @Test
  void connectOnAnAlreadyConnectedGatewayProviderReAuthorizesWithoutDowngradingStatus() {
    connectAndCompleteWithingsCallback();

    ConnectResult reauth = service.connect(IntegrationProvider.WITHINGS);

    assertThat(reauth.authorizationUrl()).isNotBlank();
    IntegrationConnection stored =
        repository
            .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
            .orElseThrow();
    assertThat(stored.status()).isEqualTo(IntegrationStatus.CONNECTED);
  }

  // --- callback --------------------------------------------------------------------------

  @Test
  void callbackWithAValidStateExchangesTokensStoresThemEncryptedAndMarksConnected() {
    ConnectResult connectResult = service.connect(IntegrationProvider.WITHINGS);
    String state = stateStore.lastIssuedState(IntegrationProvider.WITHINGS);

    IntegrationConnection connected =
        service.callback(IntegrationProvider.WITHINGS, "withings-auth-code", state);

    assertThat(connectResult.authorizationUrl()).isNotBlank();
    assertThat(connected.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(connected.connectedAt()).isNotNull();
    assertThat(withingsGateway.lastExchangedCode).isEqualTo("withings-auth-code");
    assertThat(withingsGateway.lastExchangedVerifier)
        .isEqualTo(stateStore.lastIssuedCodeVerifier(IntegrationProvider.WITHINGS));
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .contains(withingsGateway.tokensToReturn);
  }

  @Test
  void callbackWithAMismatchedStateIsRejectedAndCreatesNoConnectionOrTokens() {
    service.connect(IntegrationProvider.WITHINGS);

    assertThatThrownBy(
            () -> service.callback(IntegrationProvider.WITHINGS, "some-code", "not-the-real-state"))
        .isInstanceOf(OAuthStateException.class);

    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.PENDING);
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  @Test
  void callbackWithAnExpiredStateIsRejected() {
    stateStore.forceExpired = true;
    service.connect(IntegrationProvider.WITHINGS);
    String state = stateStore.lastIssuedState(IntegrationProvider.WITHINGS);

    assertThatThrownBy(() -> service.callback(IntegrationProvider.WITHINGS, "some-code", state))
        .isInstanceOf(OAuthStateException.class);
  }

  @Test
  void callbackReplayOfTheSameStateIsRejectedTheSecondTime() {
    connectAndCompleteWithingsCallback();
    String consumedState = stateStore.lastIssuedState(IntegrationProvider.WITHINGS);

    assertThatThrownBy(
            () ->
                service.callback(IntegrationProvider.WITHINGS, "withings-auth-code", consumedState))
        .isInstanceOf(OAuthStateException.class);
  }

  @Test
  void callbackWhenTokenExchangeFailsDoesNotMarkTheConnectionConnectedAndStoresNoTokens() {
    service.connect(IntegrationProvider.WITHINGS);
    String state = stateStore.lastIssuedState(IntegrationProvider.WITHINGS);
    withingsGateway.failExchange = true;

    assertThatThrownBy(() -> service.callback(IntegrationProvider.WITHINGS, "some-code", state))
        .isInstanceOf(ProviderOAuthException.class);

    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.PENDING);
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  @Test
  void callbackForAProviderWithoutARegisteredGatewayIsRejected() {
    assertThatThrownBy(
            () -> service.callback(IntegrationProvider.GOOGLE_FIT, "some-code", "some-state"))
        .isInstanceOf(ValidationException.class);
  }

  // --- disconnect ------------------------------------------------------------------------

  @Test
  void disconnectRemovesStoredTokensAndMarksDisconnected() {
    connectAndCompleteWithingsCallback();
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isPresent();

    IntegrationConnection disconnected = service.disconnect(IntegrationProvider.WITHINGS);

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  @Test
  void disconnectOnAProviderWithoutAGatewayMarksItDisconnected() {
    service.connect(IntegrationProvider.GOOGLE_FIT);

    IntegrationConnection disconnected = service.disconnect(IntegrationProvider.GOOGLE_FIT);

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(disconnected.connectedAt()).isNull();
  }

  @Test
  void disconnectWhenAlreadyDisconnectedIsANoOpAndLeavesNoTokens() {
    IntegrationConnection disconnected = service.disconnect(IntegrationProvider.WITHINGS);

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  // --- refreshTokenIfNeeded ----------------------------------------------------------------

  @Test
  void refreshTokenIfNeededRefreshesAnExpiredAccessTokenAndUpdatesTheStore() {
    connectAndCompleteWithingsCallback();
    ExchangedTokens refreshed =
        new ExchangedTokens("new-access-token", "new-refresh-token", LATER.plusSeconds(3600));
    withingsGateway.refreshedTokensToReturn = refreshed;

    service.refreshTokenIfNeeded(IntegrationProvider.WITHINGS, LATER.plusSeconds(1));

    assertThat(withingsGateway.refreshCalled).isTrue();
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .contains(refreshed);
  }

  @Test
  void refreshTokenIfNeededDoesNothingWhenTheAccessTokenIsStillFresh() {
    connectAndCompleteWithingsCallback();

    service.refreshTokenIfNeeded(IntegrationProvider.WITHINGS, NOW);

    assertThat(withingsGateway.refreshCalled).isFalse();
    assertThat(tokenStore.find(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .contains(withingsGateway.tokensToReturn);
  }

  @Test
  void refreshTokenIfNeededWhenNoTokensAreStoredIsANoOp() {
    service.refreshTokenIfNeeded(IntegrationProvider.WITHINGS, LATER);

    assertThat(withingsGateway.refreshCalled).isFalse();
  }

  @Test
  void
      refreshTokenIfNeededOnRefreshFailureMarksTheConnectionNeedingReauthWithoutSilentlyDropping() {
    connectAndCompleteWithingsCallback();
    withingsGateway.failRefresh = true;

    IntegrationConnection result =
        service.refreshTokenIfNeeded(IntegrationProvider.WITHINGS, LATER.plusSeconds(1));

    assertThat(result.status()).isEqualTo(IntegrationStatus.NEEDS_REAUTH);
    assertThat(result.connectedAt()).isNotNull();
    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.NEEDS_REAUTH);
  }

  // --- sync (FOR-126, tightened by FOR-131 to require CONNECTED, not just "not DISCONNECTED") -

  @Test
  void syncOnAConnectedProviderRecordsARealOutcomeWithoutFabricatingImportedCount() {
    service.connect(IntegrationProvider.GOOGLE_FIT);

    IntegrationConnection synced = service.sync(IntegrationProvider.GOOGLE_FIT);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(synced.lastSyncAt()).isNotNull();
  }

  @Test
  void syncPersistsTheUpdatedConnection() {
    service.connect(IntegrationProvider.GOOGLE_FIT);

    service.sync(IntegrationProvider.GOOGLE_FIT);

    IntegrationConnection stored =
        repository
            .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.GOOGLE_FIT)
            .orElseThrow();
    assertThat(stored.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
  }

  @Test
  void syncOnADisconnectedProviderReturnsANotConnectedOutcomeWithoutMutatingStoredState() {
    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.NOT_CONNECTED);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(
            repository.findByOwnerAndProvider(
                IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  @Test
  void syncOnAPendingProviderAwaitingCallbackReturnsANotConnectedOutcome() {
    // A mid-OAuth-handshake provider (PENDING) must not be treated as sync-able just because it
    // is not literally DISCONNECTED (FOR-131 tightened this check; see IntegrationService#sync).
    service.connect(IntegrationProvider.WITHINGS);

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.NOT_CONNECTED);
  }

  // --- sync: real Withings import (FOR-132) -----------------------------------------------

  @Test
  void syncOnAConnectedWithingsProviderImportsFetchedGroupsAsBodyMeasurements() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.groupsToReturn =
        List.of(
            new ImportedMeasureGroup(1L, measurement(70.0, "2026-07-10T08:00:00Z")),
            new ImportedMeasureGroup(2L, measurement(70.5, "2026-07-11T08:00:00Z")));

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(synced.lastSyncOutcome().importedCount()).isEqualTo(2);
    assertThat(synced.lastSyncOutcome().duplicatesSkipped()).isZero();
    assertThat(bodyMeasurementRepository.saved).hasSize(2);
    assertThat(
            markerStore.findImportedGroupIds(
                IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS))
        .containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void reSyncingTheSameFetchedGroupsImportsZeroAndReportsDuplicatesSkippedWithoutDuplicatingRows() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.groupsToReturn =
        List.of(
            new ImportedMeasureGroup(1L, measurement(70.0, "2026-07-10T08:00:00Z")),
            new ImportedMeasureGroup(2L, measurement(70.5, "2026-07-11T08:00:00Z")));
    service.sync(IntegrationProvider.WITHINGS);

    IntegrationConnection resynced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(resynced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(resynced.lastSyncOutcome().importedCount()).isZero();
    assertThat(resynced.lastSyncOutcome().duplicatesSkipped()).isEqualTo(2);
    assertThat(bodyMeasurementRepository.saved).hasSize(2); // no new rows, still just the first two
  }

  @Test
  void syncSkipsOnlyTheAlreadyImportedGroupsAndImportsTheNewOnes() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.groupsToReturn =
        List.of(new ImportedMeasureGroup(1L, measurement(70.0, "2026-07-10T08:00:00Z")));
    service.sync(IntegrationProvider.WITHINGS);
    withingsMeasuresGateway.groupsToReturn =
        List.of(
            new ImportedMeasureGroup(1L, measurement(70.0, "2026-07-10T08:00:00Z")),
            new ImportedMeasureGroup(3L, measurement(71.2, "2026-07-12T08:00:00Z")));

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().importedCount()).isEqualTo(1);
    assertThat(synced.lastSyncOutcome().duplicatesSkipped()).isEqualTo(1);
    assertThat(bodyMeasurementRepository.saved).hasSize(2);
  }

  @Test
  void syncOmitsSinceOnFirstSyncButPassesLastSyncAtOnSubsequentSyncs() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.groupsToReturn = List.of();

    service.sync(IntegrationProvider.WITHINGS);
    assertThat(withingsMeasuresGateway.lastSinceUsed).isNull();

    service.sync(IntegrationProvider.WITHINGS);
    assertThat(withingsMeasuresGateway.lastSinceUsed).isNotNull();
  }

  @Test
  void syncWhenTokenRefreshFailsReturnsNeedsReauthAndImportsNothing() {
    connectAndCompleteWithingsCallback();
    // An expiry deep in the past guarantees the refresh branch runs regardless of real wall-clock
    // time at test execution (IntegrationService#sync evaluates expiry against Instant.now()).
    tokenStore.store(
        IntegrationService.OWNER_ID,
        IntegrationProvider.WITHINGS,
        new ExchangedTokens(
            "fake-access-token", "fake-refresh-token", Instant.parse("2000-01-01T00:00:00Z")));
    withingsGateway.failRefresh = true;
    withingsMeasuresGateway.groupsToReturn =
        List.of(new ImportedMeasureGroup(1L, measurement(70.0, "2026-07-10T08:00:00Z")));

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.NEEDS_REAUTH);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(bodyMeasurementRepository.saved).isEmpty();
    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.NEEDS_REAUTH);
  }

  @Test
  void syncWhenTheProviderFetchFailsReturnsAReadableErrorOutcomeWithoutCorruptingTheConnection() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.failFetch = true;

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.ERROR);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(synced.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(bodyMeasurementRepository.saved).isEmpty();
  }

  @Test
  void syncErrorOutcomeMessageNeverContainsTheAccessToken() {
    connectAndCompleteWithingsCallback();
    withingsMeasuresGateway.failFetch = true;

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().message())
        .doesNotContain(withingsGateway.tokensToReturn.accessToken());
  }

  @Test
  void syncOnAConnectedGoogleFitProviderWithNoMeasuresGatewayStaysTheStubImport() {
    // Out of FOR-132 scope: providers without a registered ProviderMeasuresGateway keep the
    // FOR-126 stub/no-op import unchanged.
    service.connect(IntegrationProvider.GOOGLE_FIT);

    IntegrationConnection synced = service.sync(IntegrationProvider.GOOGLE_FIT);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(synced.lastSyncOutcome().duplicatesSkipped()).isZero();
    assertThat(bodyMeasurementRepository.saved).isEmpty();
  }

  private static BodyMeasurement measurement(double weightKg, String measuredAt) {
    return new BodyMeasurement(
        Instant.parse(measuredAt),
        MeasurementSource.WITHINGS,
        weightKg,
        null,
        null,
        null,
        null,
        null);
  }

  // --- port boundary guard (FOR-126, unaffected by FOR-131) -----------------------------------

  @Test
  void connectionPortExposesNoTokenAccessor() {
    // Compile-time + runtime boundary guard (ADR-004, spec FOR-126/FOR-131 tests.md): the
    // connection port must stay token-free so encrypted token storage lives entirely behind the
    // separate IntegrationTokenStore/adapter, never here.
    for (Method method : IntegrationRepository.class.getMethods()) {
      assertThat(method.getName().toLowerCase()).doesNotContain("token").doesNotContain("secret");
    }
  }

  private void connectAndCompleteWithingsCallback() {
    service.connect(IntegrationProvider.WITHINGS);
    String state = stateStore.lastIssuedState(IntegrationProvider.WITHINGS);
    service.callback(IntegrationProvider.WITHINGS, "withings-auth-code", state);
  }

  /** In-memory fake, matching {@code RecordingGoalRepository} (FOR-125). */
  private static class RecordingIntegrationRepository implements IntegrationRepository {
    final Map<String, IntegrationConnection> rows = new LinkedHashMap<>();

    @Override
    public List<IntegrationConnection> findAllByOwner(String ownerId) {
      return rows.entrySet().stream()
          .filter(e -> e.getKey().startsWith(ownerId + ":"))
          .map(Map.Entry::getValue)
          .toList();
    }

    @Override
    public Optional<IntegrationConnection> findByOwnerAndProvider(
        String ownerId, IntegrationProvider provider) {
      return Optional.ofNullable(rows.get(key(ownerId, provider)));
    }

    @Override
    public IntegrationConnection save(String ownerId, IntegrationConnection connection) {
      rows.put(key(ownerId, connection.provider()), connection);
      return connection;
    }

    private static String key(String ownerId, IntegrationProvider provider) {
      return ownerId + ":" + provider;
    }
  }

  /**
   * In-memory fake OAuth state/PKCE store. Exposes the last-issued state/verifier per provider so
   * tests can simulate the SPA relaying back exactly what the (fake) authorization URL carried,
   * without parsing a URL.
   */
  private static class FakeOAuthStateStore implements OAuthStateStore {
    private final Map<String, OAuthChallenge> challenges = new LinkedHashMap<>();
    // Remembers the last challenge issued per owner/provider even after it is consumed, so tests
    // can still inspect the state/verifier the SPA "would have relayed" post-callback.
    private final Map<String, OAuthChallenge> lastIssued = new LinkedHashMap<>();
    boolean forceExpired = false;

    @Override
    public OAuthChallenge create(String ownerId, IntegrationProvider provider, Instant now) {
      OAuthChallenge challenge =
          new OAuthChallenge(
              "state-" + provider + "-" + now.toEpochMilli(),
              "verifier-" + provider,
              "challenge-" + provider,
              forceExpired ? now.minusSeconds(1) : now.plusSeconds(600));
      challenges.put(key(ownerId, provider), challenge);
      lastIssued.put(key(ownerId, provider), challenge);
      return challenge;
    }

    @Override
    public Optional<OAuthChallenge> consume(
        String ownerId, IntegrationProvider provider, String state, Instant now) {
      OAuthChallenge challenge = challenges.get(key(ownerId, provider));
      if (challenge == null
          || !challenge.state().equals(state)
          || now.isAfter(challenge.expiresAt())) {
        return Optional.empty();
      }
      challenges.remove(key(ownerId, provider));
      return Optional.of(challenge);
    }

    String lastIssuedState(IntegrationProvider provider) {
      return lastIssued.get(key(IntegrationService.OWNER_ID, provider)).state();
    }

    String lastIssuedCodeVerifier(IntegrationProvider provider) {
      return lastIssued.get(key(IntegrationService.OWNER_ID, provider)).codeVerifier();
    }

    private static String key(String ownerId, IntegrationProvider provider) {
      return ownerId + ":" + provider;
    }
  }

  /** In-memory fake encrypted token store — no real encryption, just records what was stored. */
  private static class FakeIntegrationTokenStore implements IntegrationTokenStore {
    private final Map<String, ExchangedTokens> tokens = new LinkedHashMap<>();

    @Override
    public void store(
        String ownerId, IntegrationProvider provider, ExchangedTokens exchangedTokens) {
      tokens.put(key(ownerId, provider), exchangedTokens);
    }

    @Override
    public Optional<ExchangedTokens> find(String ownerId, IntegrationProvider provider) {
      return Optional.ofNullable(tokens.get(key(ownerId, provider)));
    }

    @Override
    public void forget(String ownerId, IntegrationProvider provider) {
      tokens.remove(key(ownerId, provider));
    }

    private static String key(String ownerId, IntegrationProvider provider) {
      return ownerId + ":" + provider;
    }
  }

  /** Fake {@link ProviderOAuthGateway} for {@link IntegrationProvider#WITHINGS} — no HTTP call. */
  private static class FakeWithingsGateway implements ProviderOAuthGateway {
    boolean failExchange = false;
    boolean failRefresh = false;
    boolean refreshCalled = false;
    String lastExchangedCode;
    String lastExchangedVerifier;
    ExchangedTokens tokensToReturn =
        new ExchangedTokens("fake-access-token", "fake-refresh-token", NOW.plusSeconds(3600));
    ExchangedTokens refreshedTokensToReturn = tokensToReturn;

    @Override
    public IntegrationProvider provider() {
      return IntegrationProvider.WITHINGS;
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
      return "https://account.withings.com/oauth2_user/authorize2?state="
          + state
          + "&code_challenge="
          + codeChallenge;
    }

    @Override
    public ExchangedTokens exchangeAuthorizationCode(String code, String codeVerifier) {
      lastExchangedCode = code;
      lastExchangedVerifier = codeVerifier;
      if (failExchange) {
        throw new ProviderOAuthException("Withings token exchange failed");
      }
      return tokensToReturn;
    }

    @Override
    public ExchangedTokens refreshTokens(String refreshToken) {
      refreshCalled = true;
      if (failRefresh) {
        throw new ProviderOAuthException("Withings token refresh failed");
      }
      return refreshedTokensToReturn;
    }
  }

  /** In-memory fake {@link ImportedMeasureMarkerStore} (FOR-132) — a plain in-memory set. */
  private static class FakeImportedMeasureMarkerStore implements ImportedMeasureMarkerStore {
    private final Set<String> marked = new LinkedHashSet<>();

    @Override
    public Set<Long> findImportedGroupIds(String ownerId, IntegrationProvider provider) {
      Set<Long> ids = new LinkedHashSet<>();
      for (String entry : marked) {
        String[] parts = entry.split(":", 3);
        if (parts[0].equals(ownerId) && parts[1].equals(provider.name())) {
          ids.add(Long.valueOf(parts[2]));
        }
      }
      return ids;
    }

    @Override
    public void markImported(
        String ownerId, IntegrationProvider provider, long groupId, Instant importedAt) {
      marked.add(ownerId + ":" + provider.name() + ":" + groupId);
    }
  }

  /**
   * In-memory fake {@link BodyMeasurementRepository} (FOR-132) — records every saved measurement.
   */
  private static class FakeBodyMeasurementRepository implements BodyMeasurementRepository {
    final List<BodyMeasurement> saved = new ArrayList<>();

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      return List.copyOf(saved);
    }
  }

  /**
   * Fake {@link ProviderMeasuresGateway} for {@link IntegrationProvider#WITHINGS} (FOR-132) — no
   * HTTP call, returns a configurable, test-controlled list of already-mapped groups.
   */
  private static class FakeWithingsMeasuresGateway implements ProviderMeasuresGateway {
    List<ImportedMeasureGroup> groupsToReturn = List.of();
    boolean failFetch = false;
    Instant lastSinceUsed;

    @Override
    public IntegrationProvider provider() {
      return IntegrationProvider.WITHINGS;
    }

    @Override
    public List<ImportedMeasureGroup> fetchMeasureGroups(String accessToken, Instant since) {
      lastSinceUsed = since;
      if (failFetch) {
        throw new ProviderSyncException("Withings measures fetch failed");
      }
      return groupsToReturn;
    }
  }
}

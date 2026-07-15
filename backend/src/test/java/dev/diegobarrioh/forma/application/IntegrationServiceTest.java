package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link IntegrationService} (FOR-126): owner-scoped status/
 * connect/disconnect/sync, with a stub sync that never fabricates an imported count. Uses a
 * hand-rolled in-memory fake (no Spring, no Mockito), matching {@code GoalServiceTest} (FOR-125,
 * ADR-007).
 */
class IntegrationServiceTest {

  private final RecordingIntegrationRepository repository = new RecordingIntegrationRepository();
  private final IntegrationService service = new IntegrationService(repository);

  @Test
  void statusListsEveryKnownProviderDefaultingToDisconnectedWhenNoneStored() {
    List<IntegrationConnection> status = service.status();

    assertThat(status).hasSize(IntegrationProvider.values().length);
    assertThat(status)
        .allSatisfy(c -> assertThat(c.status()).isEqualTo(IntegrationStatus.DISCONNECTED));
  }

  @Test
  void connectMarksAProviderConnectedAndPersistsIt() {
    IntegrationConnection connected = service.connect(IntegrationProvider.WITHINGS);

    assertThat(connected.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(connected.connectedAt()).isNotNull();
    assertThat(
            repository
                .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
                .orElseThrow()
                .status())
        .isEqualTo(IntegrationStatus.CONNECTED);
  }

  @Test
  void connectIsOwnerScoped() {
    service.connect(IntegrationProvider.WITHINGS);

    assertThat(repository.findByOwnerAndProvider("someone-else", IntegrationProvider.WITHINGS))
        .isEmpty();
  }

  @Test
  void connectWhenAlreadyConnectedIsIdempotent() {
    IntegrationConnection first = service.connect(IntegrationProvider.WITHINGS);

    IntegrationConnection second = service.connect(IntegrationProvider.WITHINGS);

    assertThat(second.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(second.connectedAt()).isEqualTo(first.connectedAt());
  }

  @Test
  void disconnectMarksAConnectedProviderDisconnected() {
    service.connect(IntegrationProvider.WITHINGS);

    IntegrationConnection disconnected = service.disconnect(IntegrationProvider.WITHINGS);

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(disconnected.connectedAt()).isNull();
  }

  @Test
  void disconnectWhenAlreadyDisconnectedIsANoOp() {
    IntegrationConnection disconnected = service.disconnect(IntegrationProvider.WITHINGS);

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
  }

  @Test
  void syncOnAConnectedProviderRecordsARealOutcomeWithoutFabricatingImportedCount() {
    service.connect(IntegrationProvider.WITHINGS);

    IntegrationConnection synced = service.sync(IntegrationProvider.WITHINGS);

    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
    assertThat(synced.lastSyncAt()).isNotNull();
  }

  @Test
  void syncPersistsTheUpdatedConnection() {
    service.connect(IntegrationProvider.WITHINGS);

    service.sync(IntegrationProvider.WITHINGS);

    IntegrationConnection stored =
        repository
            .findByOwnerAndProvider(IntegrationService.OWNER_ID, IntegrationProvider.WITHINGS)
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
  void portExposesNoTokenAccessor() {
    // Compile-time + runtime boundary guard (ADR-004, spec FOR-126 tests.md Application Tests):
    // the port must stay token-free so slices 2-3 can add encrypted token storage inside the
    // adapter without ever changing this interface.
    for (Method method : IntegrationRepository.class.getMethods()) {
      assertThat(method.getName().toLowerCase()).doesNotContain("token").doesNotContain("secret");
    }
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
}

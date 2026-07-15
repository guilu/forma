package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link IntegrationConnection} (FOR-126): state transitions between {@link
 * IntegrationStatus#DISCONNECTED} and {@link IntegrationStatus#CONNECTED}, and sync bookkeeping.
 * Deliberately carries no token/secret field or accessor (ADR-004, spec FOR-126 boundary rule) —
 * this is asserted explicitly so slices 2-3 cannot silently reintroduce one here.
 */
class IntegrationConnectionTest {

  private static final Instant NOW = Instant.parse("2026-07-15T08:00:00Z");
  private static final Instant LATER = Instant.parse("2026-07-15T09:00:00Z");

  @Test
  void rejectsNullProvider() {
    assertThatThrownBy(
            () -> new IntegrationConnection(null, IntegrationStatus.DISCONNECTED, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void defaultsStatusToDisconnectedWhenNotProvided() {
    IntegrationConnection connection =
        new IntegrationConnection(IntegrationProvider.WITHINGS, null, null, null, null);

    assertThat(connection.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
  }

  @Test
  void disconnectedDefaultIsDisconnectedWithNoTimestamps() {
    IntegrationConnection connection =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS);

    assertThat(connection.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(connection.connectedAt()).isNull();
    assertThat(connection.lastSyncAt()).isNull();
    assertThat(connection.lastSyncOutcome()).isNull();
  }

  @Test
  void connectMarksTheProviderConnectedAndSetsConnectedAt() {
    IntegrationConnection disconnected =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS);

    IntegrationConnection connected = disconnected.connect(NOW);

    assertThat(connected.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(connected.connectedAt()).isEqualTo(NOW);
  }

  @Test
  void connectWhenAlreadyConnectedIsIdempotentAndKeepsTheOriginalConnectedAt() {
    IntegrationConnection connected =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS).connect(NOW);

    IntegrationConnection reconnected = connected.connect(LATER);

    assertThat(reconnected.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(reconnected.connectedAt()).isEqualTo(NOW);
  }

  @Test
  void disconnectMarksTheProviderDisconnectedAndClearsConnectedAt() {
    IntegrationConnection connected =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS).connect(NOW);

    IntegrationConnection disconnected = connected.disconnect();

    assertThat(disconnected.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(disconnected.connectedAt()).isNull();
  }

  @Test
  void disconnectWhenAlreadyDisconnectedIsANoOp() {
    IntegrationConnection disconnected =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS);

    IntegrationConnection stillDisconnected = disconnected.disconnect();

    assertThat(stillDisconnected).isEqualTo(disconnected);
  }

  @Test
  void disconnectPreservesSyncHistory() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, null);
    IntegrationConnection synced =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS)
            .connect(NOW)
            .withSyncOutcome(LATER, outcome);

    IntegrationConnection disconnected = synced.disconnect();

    assertThat(disconnected.lastSyncAt()).isEqualTo(LATER);
    assertThat(disconnected.lastSyncOutcome()).isEqualTo(outcome);
  }

  @Test
  void withSyncOutcomeRecordsLastSyncAtAndOutcomeWithoutFabricatingImportedCount() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, null);
    IntegrationConnection connected =
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS).connect(NOW);

    IntegrationConnection synced = connected.withSyncOutcome(LATER, outcome);

    assertThat(synced.lastSyncAt()).isEqualTo(LATER);
    assertThat(synced.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(synced.lastSyncOutcome().importedCount()).isZero();
  }

  @Test
  void carriesNoTokenOrSecretField() {
    // Compile-time boundary guard (ADR-004 + spec FOR-126): fail loudly if a future edit adds a
    // token/secret record component to the domain aggregate directly (slices 2-3 must add token
    // storage in the adapter, never here).
    for (var component : IntegrationConnection.class.getRecordComponents()) {
      assertThat(component.getName().toLowerCase())
          .doesNotContain("token")
          .doesNotContain("secret");
    }
  }
}

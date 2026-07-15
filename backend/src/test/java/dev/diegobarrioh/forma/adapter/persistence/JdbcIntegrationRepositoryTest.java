package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.IntegrationRepository;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcIntegrationRepository} (FOR-126). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V12), like the FOR-107/FOR-125 tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcIntegrationRepositoryTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final Instant CONNECTED_AT = Instant.parse("2026-07-15T08:00:00Z");
  private static final Instant SYNCED_AT = Instant.parse("2026-07-15T09:00:00Z");

  @Autowired private IntegrationRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM integration_connection");
  }

  @Test
  void findAllByOwnerIsEmptyOnACleanDatabase() {
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void findByOwnerAndProviderIsEmptyWhenNeverStored() {
    assertThat(repository.findByOwnerAndProvider(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void saveInsertsANewConnectionAndRoundTripsIt() {
    IntegrationConnection connection =
        new IntegrationConnection(
            IntegrationProvider.WITHINGS, IntegrationStatus.CONNECTED, CONNECTED_AT, null, null);

    repository.save(OWNER, connection);

    IntegrationConnection read =
        repository.findByOwnerAndProvider(OWNER, IntegrationProvider.WITHINGS).orElseThrow();
    assertThat(read.provider()).isEqualTo(IntegrationProvider.WITHINGS);
    assertThat(read.status()).isEqualTo(IntegrationStatus.CONNECTED);
    assertThat(read.connectedAt()).isEqualTo(CONNECTED_AT);
    assertThat(read.lastSyncAt()).isNull();
    assertThat(read.lastSyncOutcome()).isNull();
  }

  @Test
  void saveRoundTripsASyncOutcome() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, null);
    IntegrationConnection connection =
        new IntegrationConnection(
            IntegrationProvider.WITHINGS,
            IntegrationStatus.CONNECTED,
            CONNECTED_AT,
            SYNCED_AT,
            outcome);

    repository.save(OWNER, connection);

    IntegrationConnection read =
        repository.findByOwnerAndProvider(OWNER, IntegrationProvider.WITHINGS).orElseThrow();
    assertThat(read.lastSyncAt()).isEqualTo(SYNCED_AT);
    assertThat(read.lastSyncOutcome().result()).isEqualTo(SyncResult.OK);
    assertThat(read.lastSyncOutcome().importedCount()).isZero();
    assertThat(read.lastSyncOutcome().message()).isNull();
  }

  @Test
  void saveUpdatesAnExistingRowInsteadOfDuplicatingIt() {
    repository.save(OWNER, IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS));

    repository.save(
        OWNER,
        new IntegrationConnection(
            IntegrationProvider.WITHINGS, IntegrationStatus.CONNECTED, CONNECTED_AT, null, null));

    List<IntegrationConnection> all = repository.findAllByOwner(OWNER);
    assertThat(all).hasSize(1);
    assertThat(all.get(0).status()).isEqualTo(IntegrationStatus.CONNECTED);
  }

  @Test
  void findAllByOwnerNeverReturnsAnotherOwnersConnections() {
    repository.save(
        OTHER_OWNER,
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS)
            .connect(CONNECTED_AT));

    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void findByOwnerAndProviderNeverReturnsAnotherOwnersConnection() {
    repository.save(
        OTHER_OWNER,
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS)
            .connect(CONNECTED_AT));

    Optional<IntegrationConnection> found =
        repository.findByOwnerAndProvider(OWNER, IntegrationProvider.WITHINGS);

    assertThat(found).isEmpty();
  }

  @Test
  void aDisconnectedConnectionWithNoSyncHistoryRoundTripsWithNullFields() {
    repository.save(
        OWNER, IntegrationConnection.disconnectedDefault(IntegrationProvider.GOOGLE_FIT));

    IntegrationConnection read =
        repository.findByOwnerAndProvider(OWNER, IntegrationProvider.GOOGLE_FIT).orElseThrow();

    assertThat(read.status()).isEqualTo(IntegrationStatus.DISCONNECTED);
    assertThat(read.connectedAt()).isNull();
    assertThat(read.lastSyncAt()).isNull();
    assertThat(read.lastSyncOutcome()).isNull();
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.IntegrationRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcIntegrationRepository} (FOR-126). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V12), like the FOR-107/FOR-125 tests.
 *
 * <p>FOR-145b-2 (migration V28): {@code integration_connection.user_id} FK-references {@code
 * users(id)}, so {@code OTHER_OWNER} must be a real seeded row (matching {@code
 * JdbcGoalRepositoryTest}'s pattern for Class-A tables). {@code OWNER} reuses the always-present
 * legacy placeholder account.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcIntegrationRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();
  private static final Instant CONNECTED_AT = Instant.parse("2026-07-15T08:00:00Z");
  private static final Instant SYNCED_AT = Instant.parse("2026-07-15T09:00:00Z");

  @Autowired private IntegrationRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedTables() {
    jdbcTemplate.update("DELETE FROM integration_connection");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "integration-connection-other-owner@test.local",
        "!");
  }

  @AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM integration_connection");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
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
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, 0, null);
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
    assertThat(read.lastSyncOutcome().duplicatesSkipped()).isZero();
    assertThat(read.lastSyncOutcome().message()).isNull();
  }

  @Test
  void saveRoundTripsANonZeroDuplicatesSkippedCount() {
    // FOR-132: duplicatesSkipped is a new column (migration V16) alongside the existing
    // last_sync_* columns this repository already flattens SyncOutcome onto.
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 3, 12, null);
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
    assertThat(read.lastSyncOutcome().importedCount()).isEqualTo(3);
    assertThat(read.lastSyncOutcome().duplicatesSkipped()).isEqualTo(12);
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

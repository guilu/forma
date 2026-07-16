package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ImportedMeasureMarkerStore;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcImportedMeasureMarkerStore} (FOR-132, migration V16). Runs
 * against the in-memory PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like {@code
 * JdbcIntegrationTokenStoreTest} (FOR-131).
 *
 * <p>The headline assertion ({@code reSyncingTheSameGroupTwiceStaysIdempotent}) is {@code
 * tests.md}'s "Dedup keyed on Withings grpid" — marking the same group twice must never throw or
 * duplicate the row, since {@link dev.diegobarrioh.forma.application.IntegrationService#sync} calls
 * {@code markImported} once per newly-imported group with no separate "already exists" check.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcImportedMeasureMarkerStoreTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final Instant IMPORTED_AT = Instant.parse("2026-07-16T12:00:00Z");

  @Autowired private ImportedMeasureMarkerStore markerStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM integration_measure_marker");
  }

  @Test
  void findImportedGroupIdsIsEmptyWhenNoneMarked() {
    assertThat(markerStore.findImportedGroupIds(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void markImportedThenFindReturnsTheMarkedGroupId() {
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 123456789L, IMPORTED_AT);

    assertThat(markerStore.findImportedGroupIds(OWNER, IntegrationProvider.WITHINGS))
        .containsExactly(123456789L);
  }

  @Test
  void marksMultipleDistinctGroupIds() {
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 1L, IMPORTED_AT);
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 2L, IMPORTED_AT);
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 3L, IMPORTED_AT);

    assertThat(markerStore.findImportedGroupIds(OWNER, IntegrationProvider.WITHINGS))
        .containsExactlyInAnyOrder(1L, 2L, 3L);
  }

  @Test
  void reSyncingTheSameGroupTwiceStaysIdempotent() {
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 42L, IMPORTED_AT);

    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 42L, IMPORTED_AT.plusSeconds(60));

    Set<Long> ids = markerStore.findImportedGroupIds(OWNER, IntegrationProvider.WITHINGS);
    assertThat(ids).containsExactly(42L);
    Integer rowCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM integration_measure_marker "
                + "WHERE owner_id = ? AND provider = ? AND grpid = ?",
            Integer.class,
            OWNER,
            IntegrationProvider.WITHINGS.name(),
            42L);
    assertThat(rowCount).isEqualTo(1);
  }

  @Test
  void findImportedGroupIdsNeverReturnsAnotherOwnersMarkers() {
    markerStore.markImported(OTHER_OWNER, IntegrationProvider.WITHINGS, 99L, IMPORTED_AT);

    assertThat(markerStore.findImportedGroupIds(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void findImportedGroupIdsIsScopedByProvider() {
    markerStore.markImported(OWNER, IntegrationProvider.WITHINGS, 7L, IMPORTED_AT);

    assertThat(markerStore.findImportedGroupIds(OWNER, IntegrationProvider.GOOGLE_FIT)).isEmpty();
  }
}

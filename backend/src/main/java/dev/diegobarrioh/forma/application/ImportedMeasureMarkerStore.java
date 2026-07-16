package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.util.Set;

/**
 * Port for the idempotent duplicate-detection markers a real provider sync writes (FOR-132,
 * ADR-004: "Synchronization must be idempotent. Duplicate detection is mandatory for imported
 * records."). Persists which provider measure-group ids have already been imported, keyed by {@code
 * (ownerId, provider, groupId)}, in a table entirely separate from {@code body_measurements} —
 * {@code BodyMeasurement} carries no external id by design (spec FOR-132), so this is the only
 * place the dedup key lives.
 *
 * <p>Owned by the application/domain side; the adapter implements it (ADR-001), mirroring {@link
 * IntegrationTokenStore}'s and {@link IntegrationRepository}'s split. Every method is owner-scoped
 * (ADR-002).
 */
public interface ImportedMeasureMarkerStore {

  /**
   * All measure-group ids already imported for {@code ownerId}/{@code provider}, so {@link
   * IntegrationService#sync} can skip them without a per-group round trip (the expected per-user
   * history is small, matching {@code IntegrationRepository}'s "tiny per-user connection set"
   * assumption).
   */
  Set<Long> findImportedGroupIds(String ownerId, IntegrationProvider provider);

  /**
   * Records that {@code groupId} was imported at {@code importedAt}, so a later sync of the same
   * fixture/history skips it (spec FOR-132 tests.md: "second sync of the same fixture imports 0,
   * duplicatesSkipped = N — NO duplicate BodyMeasurements created"). Idempotent: marking an
   * already-marked group again is a safe no-op.
   */
  void markImported(String ownerId, IntegrationProvider provider, long groupId, Instant importedAt);
}

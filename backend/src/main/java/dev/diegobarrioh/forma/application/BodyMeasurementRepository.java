package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import java.util.List;
import java.util.UUID;

/**
 * Port for persisting and reading {@link BodyMeasurement} records (FOR-16).
 *
 * <p>Owned by the application/domain side; adapters implement it (ADR-001). The domain type stays
 * framework-free — this interface speaks only in domain objects, never in rows or SQL types.
 *
 * <p>{@code userId} is a real account id (FOR-145c "gap table" closure, migration V30) — {@code
 * body_measurements.user_id UUID}, FK-referencing {@code users(id)}, backfilled onto the legacy
 * placeholder account. Before this slice the table had NO owner-scoping at all.
 */
public interface BodyMeasurementRepository {

  /**
   * Persists one measurement for {@code userId}.
   *
   * @param userId the owning account's id
   * @param measurement the measurement to store; must not be {@code null}
   */
  void save(UUID userId, BodyMeasurement measurement);

  /**
   * Lists {@code userId}'s stored measurements, most recent first (ordered by {@code measuredAt}
   * descending).
   *
   * @param userId the owning account's id
   * @return the measurements, newest first; empty when none are stored
   */
  List<BodyMeasurement> list(UUID userId);
}

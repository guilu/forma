package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import java.util.List;

/**
 * Port for persisting and reading {@link BodyMeasurement} records (FOR-16).
 *
 * <p>Owned by the application/domain side; adapters implement it (ADR-001). The domain type stays
 * framework-free — this interface speaks only in domain objects, never in rows or SQL types.
 */
public interface BodyMeasurementRepository {

  /**
   * Persists one measurement.
   *
   * @param measurement the measurement to store; must not be {@code null}
   */
  void save(BodyMeasurement measurement);

  /**
   * Lists all stored measurements, most recent first (ordered by {@code measuredAt} descending).
   *
   * @return the measurements, newest first; empty when none are stored
   */
  List<BodyMeasurement> list();
}

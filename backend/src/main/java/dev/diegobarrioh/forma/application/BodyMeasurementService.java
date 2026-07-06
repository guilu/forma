package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for body measurements (FOR-17).
 *
 * <p>Keeps the "a measurement created through the API is always {@link MeasurementSource#MANUAL}"
 * rule out of the controller (ADR-001: controllers stay thin, business rules live in the
 * application/domain layer). Delegates persistence to the FOR-16 {@link BodyMeasurementRepository}
 * port and returns domain objects, so derived values come from {@link BodyMeasurement} and are
 * never recomputed in the delivery layer.
 */
@Service
public class BodyMeasurementService {

  private final BodyMeasurementRepository repository;

  public BodyMeasurementService(BodyMeasurementRepository repository) {
    this.repository = repository;
  }

  /**
   * Records a manually entered measurement and returns it (with derived values).
   *
   * <p>{@code source} is fixed to {@link MeasurementSource#MANUAL}; callers cannot supply it.
   */
  public BodyMeasurement createManual(
      Instant measuredAt, double weightKg, Double bodyFatPercentage, Double bmi, String notes) {
    BodyMeasurement measurement =
        new BodyMeasurement(
            measuredAt, MeasurementSource.MANUAL, weightKg, bodyFatPercentage, bmi, notes);
    repository.save(measurement);
    return measurement;
  }

  /** Lists stored measurements, most recent first (FOR-16 default order). */
  public List<BodyMeasurement> list() {
    return repository.list();
  }
}

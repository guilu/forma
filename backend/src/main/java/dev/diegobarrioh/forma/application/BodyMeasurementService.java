package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application use cases for body measurements (FOR-17).
 *
 * <p>Keeps the "a measurement created through the API is always {@link MeasurementSource#MANUAL}"
 * rule out of the controller (ADR-001: controllers stay thin, business rules live in the
 * application/domain layer). Delegates persistence to the FOR-16 {@link BodyMeasurementRepository}
 * port and returns domain objects, so derived values come from {@link BodyMeasurement} and are
 * never recomputed in the delivery layer.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V30): this "gap table" service had ZERO
 * owner-scoping before this slice. Every use case now resolves the caller's account id via {@link
 * CurrentUserProvider} and passes it to the repository on every call.
 */
@Service
public class BodyMeasurementService {

  private final BodyMeasurementRepository repository;
  private final UserProfileRepository profileRepository;
  private final CurrentUserProvider currentUserProvider;

  public BodyMeasurementService(
      BodyMeasurementRepository repository,
      UserProfileRepository profileRepository,
      CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.profileRepository = profileRepository;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Records a manually entered measurement and returns it (with derived values).
   *
   * <p>{@code source} is fixed to {@link MeasurementSource#MANUAL}; callers cannot supply it.
   *
   * <p>{@code muscleMassKg} and {@code waterPercentage} are optional (FOR-100); {@code null} means
   * "not provided", never a fabricated value.
   */
  public BodyMeasurement createManual(
      Instant measuredAt,
      double weightKg,
      Double bodyFatPercentage,
      Double bmi,
      Double muscleMassKg,
      Double waterPercentage,
      String notes) {
    BodyMeasurement measurement =
        new BodyMeasurement(
            measuredAt,
            MeasurementSource.MANUAL,
            weightKg,
            bodyFatPercentage,
            bmi,
            muscleMassKg,
            waterPercentage,
            notes);
    repository.save(currentUserProvider.currentUserId(), measurement);
    return measurement;
  }

  /**
   * Lists the caller's stored measurements, most recent first (FOR-16 default order), each paired
   * with its row id so the delivery layer can expose something to delete (FOR-187).
   *
   * <p>When a stored/imported measurement has no BMI but the caller's profile has a height, the
   * read model derives BMI as {@code weightKg / heightMeters^2}. The derived value is returned only
   * in this read path; it is not written back to {@code body_measurements}, so provider/manual
   * source data remains untouched and historical Withings rows become display-complete without a
   * re-sync.
   */
  public List<StoredBodyMeasurement> list() {
    UUID userId = currentUserProvider.currentUserId();
    Double heightCm =
        profileRepository.find(userId).map(profile -> profile.heightCm()).orElse(null);
    return repository.listWithIds(userId).stream()
        .map(stored -> withDerivedBmi(stored, heightCm))
        .toList();
  }

  private static StoredBodyMeasurement withDerivedBmi(
      StoredBodyMeasurement stored, Double heightCm) {
    BodyMeasurement measurement = stored.measurement();
    if (measurement.bmi() != null || heightCm == null || heightCm <= 0) {
      return stored;
    }
    double heightMeters = heightCm / 100.0;
    double derivedBmi = measurement.weightKg() / (heightMeters * heightMeters);
    return new StoredBodyMeasurement(
        stored.id(),
        new BodyMeasurement(
            measurement.measuredAt(),
            measurement.source(),
            measurement.weightKg(),
            measurement.bodyFatPercentage(),
            derivedBmi,
            measurement.muscleMassKg(),
            measurement.waterPercentage(),
            measurement.notes()));
  }

  /**
   * Deletes one of the caller's measurements (FOR-187).
   *
   * <p>The delete is scoped to the caller in the repository, so this never has to read the row
   * first to check ownership — and never learns whether a rejected id belongs to someone else or to
   * nobody. Both raise {@link NotFoundException}, which the delivery layer maps to 404 (ADR-002: no
   * existence leak across accounts).
   *
   * @throws NotFoundException when no measurement of the caller's has that id
   */
  public void delete(UUID id) {
    if (!repository.delete(currentUserProvider.currentUserId(), id)) {
      throw new NotFoundException("No existe una medición con ese identificador.");
    }
  }
}

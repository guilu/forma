package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BodyMeasurementService} (FOR-17). Uses a hand-rolled in-memory fake
 * repository (no Spring, no Mockito) so the use-case rules are verified in isolation (ADR-007).
 */
class BodyMeasurementServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private final RecordingRepository repository = new RecordingRepository();
  private final RecordingUserProfileRepository profileRepository = new RecordingUserProfileRepository();
  private final BodyMeasurementService service =
      new BodyMeasurementService(repository, profileRepository, () -> USER_ID);

  @Test
  void createManualBuildsManualMeasurementAndPersistsIt() {
    BodyMeasurement created =
        service.createManual(
            Instant.parse("2026-07-05T08:00:00Z"), 78.4, 18.2, 23.9, null, null, "Morning, fasted");

    // The use case fixes the source to MANUAL regardless of caller input.
    assertThat(created.source()).isEqualTo(MeasurementSource.MANUAL);
    assertThat(created.fatMassKg()).isPresent();
    // It persists exactly what it returns.
    assertThat(repository.saved).containsExactly(created);
  }

  @Test
  void createManualPersistsMuscleMassAndWaterPercentageWhenProvided() {
    BodyMeasurement created =
        service.createManual(
            Instant.parse("2026-07-11T08:00:00Z"), 73.6, 14.7, 22.7, 62.8, 58.0, null);

    assertThat(created.muscleMassKg()).isEqualTo(62.8);
    assertThat(created.waterPercentage()).isEqualTo(58.0);
    assertThat(repository.saved).containsExactly(created);
  }

  @Test
  void listDelegatesToRepositoryAndCarriesTheIds() {
    BodyMeasurement stored = measurement("2026-07-05T08:00:00Z");
    repository.saved.add(stored);

    assertThat(service.list())
        .extracting(StoredBodyMeasurement::measurement)
        .containsExactly(stored);
    assertThat(service.list()).extracting(StoredBodyMeasurement::id).doesNotContainNull();
  }

  @Test
  void listDerivesMissingBmiFromProfileHeightWithoutPersistingIt() {
    BodyMeasurement stored =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.WITHINGS,
            73.6,
            14.7,
            null,
            62.8,
            null,
            null);
    repository.saved.add(stored);
    profileRepository.profile =
        Optional.of(
            new UserProfile(
                USER_ID,
                "Diego",
                null,
                null,
                null,
                180.0,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null));

    List<StoredBodyMeasurement> listed = service.list();

    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).measurement().bmi()).isEqualTo(22.716049382716047);
    assertThat(repository.saved.get(0).bmi()).isNull();
  }

  @Test
  void listDoesNotDeriveBmiWhenProfileHeightIsMissing() {
    BodyMeasurement stored =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.WITHINGS,
            73.6,
            14.7,
            null,
            62.8,
            null,
            null);
    repository.saved.add(stored);

    assertThat(service.list().get(0).measurement().bmi()).isNull();
  }

  @Test
  void deleteRemovesTheCallersMeasurement() {
    repository.saved.add(measurement("2026-07-05T08:00:00Z"));
    UUID id = service.list().get(0).id();

    service.delete(id);

    assertThat(service.list()).isEmpty();
    assertThat(repository.deletedBy).containsExactly(USER_ID);
  }

  /**
   * A measurement that does not exist and one that belongs to someone else are the same answer: the
   * repository scopes the delete by owner, so the service never learns which it was, and a caller
   * cannot probe for another account's ids (ADR-002).
   */
  @Test
  void deleteReportsNotFoundWhenNothingWasRemoved() {
    assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  private static BodyMeasurement measurement(String measuredAt) {
    return new BodyMeasurement(
        Instant.parse(measuredAt), MeasurementSource.MANUAL, 80.0, 25.0, 24.0, null, null, null);
  }

  private static final class RecordingUserProfileRepository implements UserProfileRepository {
    private Optional<UserProfile> profile = Optional.empty();
    private final List<UserProfile> saved = new ArrayList<>();

    @Override
    public Optional<UserProfile> find(UUID userId) {
      return profile;
    }

    @Override
    public void save(UserProfile profile) {
      saved.add(profile);
      this.profile = Optional.of(profile);
    }
  }

  /** In-memory {@link BodyMeasurementRepository} that records saves and returns them on list. */
  private static final class RecordingRepository implements BodyMeasurementRepository {
    private final List<BodyMeasurement> saved = new ArrayList<>();
    private final Map<UUID, BodyMeasurement> byId = new LinkedHashMap<>();
    private final List<UUID> deletedBy = new ArrayList<>();

    @Override
    public void save(UUID userId, BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list(UUID userId) {
      return List.copyOf(saved);
    }

    @Override
    public List<StoredBodyMeasurement> listWithIds(UUID userId) {
      // Ids are assigned on first read and kept stable, mirroring stored rows.
      for (BodyMeasurement measurement : saved) {
        if (!byId.containsValue(measurement)) {
          byId.put(UUID.randomUUID(), measurement);
        }
      }
      return byId.entrySet().stream()
          .map(entry -> new StoredBodyMeasurement(entry.getKey(), entry.getValue()))
          .toList();
    }

    @Override
    public boolean delete(UUID userId, UUID id) {
      deletedBy.add(userId);
      BodyMeasurement removed = byId.remove(id);
      if (removed == null) {
        return false;
      }
      saved.remove(removed);
      return true;
    }
  }
}

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BodyMeasurementService} (FOR-17). Uses a hand-rolled in-memory fake
 * repository (no Spring, no Mockito) so the use-case rules are verified in isolation (ADR-007).
 */
class BodyMeasurementServiceTest {

  private final RecordingRepository repository = new RecordingRepository();
  private final BodyMeasurementService service = new BodyMeasurementService(repository);

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
  void listDelegatesToRepository() {
    BodyMeasurement stored =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.MANUAL,
            80.0,
            25.0,
            24.0,
            null,
            null,
            null);
    repository.saved.add(stored);

    assertThat(service.list()).containsExactly(stored);
  }

  /** In-memory {@link BodyMeasurementRepository} that records saves and returns them on list. */
  private static final class RecordingRepository implements BodyMeasurementRepository {
    private final List<BodyMeasurement> saved = new ArrayList<>();

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      return List.copyOf(saved);
    }
  }
}

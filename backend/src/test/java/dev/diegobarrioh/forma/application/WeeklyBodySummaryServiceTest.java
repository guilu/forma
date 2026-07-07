package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyBodySummaryService} (FOR-21). Uses an in-memory fake repository (no
 * Spring, no Mockito) to verify the use case reads via the port and produces a summary (ADR-007).
 */
class WeeklyBodySummaryServiceTest {

  private final FakeRepository repository = new FakeRepository();
  private final WeeklyBodySummaryService service = new WeeklyBodySummaryService(repository);

  private static BodyMeasurement measurement(String isoDate, double weightKg, double bodyFat) {
    return new BodyMeasurement(
        Instant.parse(isoDate + "T08:00:00Z"),
        MeasurementSource.MANUAL,
        weightKg,
        bodyFat,
        22.7,
        null);
  }

  @Test
  void producesSummaryFromRepositoryHistory() {
    // Newest-first, as the repository returns them.
    repository.measurements.add(measurement("2026-07-08", 73.6, 14.7));
    repository.measurements.add(measurement("2026-07-01", 74.1, 15.0));

    WeeklyBodySummary summary = service.currentSummary();

    assertThat(summary.latestWeightKg()).isEqualTo(73.6);
    assertThat(summary.weeklyWeightChangeKg()).isCloseTo(-0.5, within(1e-9));
    assertThat(summary.comparisonDays()).isEqualTo(7);
  }

  @Test
  void reportsNoDataWhenRepositoryIsEmpty() {
    WeeklyBodySummary summary = service.currentSummary();

    assertThat(summary.latestWeightKg()).isNull();
    assertThat(summary.message()).isEqualTo("Aún no hay mediciones para resumir.");
  }

  /** In-memory repository returning its measurements in insertion (newest-first) order. */
  private static final class FakeRepository implements BodyMeasurementRepository {
    private final List<BodyMeasurement> measurements = new ArrayList<>();

    @Override
    public void save(BodyMeasurement measurement) {
      measurements.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      return List.copyOf(measurements);
    }
  }
}

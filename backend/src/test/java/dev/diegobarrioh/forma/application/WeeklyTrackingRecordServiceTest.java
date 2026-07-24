package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrackingRecordService} (FOR-155). Uses a hand-rolled in-memory fake
 * repository (no Spring, no Mockito), mirroring {@code BodyMeasurementServiceTest} (ADR-007).
 */
class WeeklyTrackingRecordServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private final RecordingRepository repository = new RecordingRepository();
  private final WeeklyTrackingRecordService service =
      new WeeklyTrackingRecordService(repository, () -> USER_ID);

  @Test
  void listDelegatesToRepositoryOwnerScoped() {
    WeeklyTrackingRecord stored =
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, null);
    repository.byWeek.put(1, stored);

    assertThat(service.list()).containsExactly(stored);
    assertThat(repository.lastOwnerId).isEqualTo(USER_ID);
  }

  @Test
  void listReturnsEmptyWhenNoRecordsExist() {
    // SEGUIMIENTO starts empty (spec FOR-155) — this is the default, never an error.
    assertThat(service.list()).isEmpty();
  }

  @Test
  void saveUpsertsAndReturnsTheRecord() {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, "note");

    WeeklyTrackingRecord result = service.save(record);

    assertThat(result).isEqualTo(record);
    assertThat(repository.upserted).containsExactly(record);
    assertThat(repository.lastOwnerId).isEqualTo(USER_ID);
  }

  @Test
  void getByWeekReturnsStoredRecord() {
    WeeklyTrackingRecord stored =
        new WeeklyTrackingRecord(
            3, LocalDate.parse("2026-07-20"), null, null, null, null, null, null, null);
    repository.byWeek.put(3, stored);

    assertThat(service.getByWeek(3)).isEqualTo(stored);
  }

  @Test
  void getByWeekThrowsNotFoundWhenMissing() {
    assertThatThrownBy(() -> service.getByWeek(5)).isInstanceOf(NotFoundException.class);
  }

  /** In-memory {@link WeeklyTrackingRecordRepository} fake, owner-scoped like the JDBC adapter. */
  private static final class RecordingRepository implements WeeklyTrackingRecordRepository {
    private final java.util.Map<Integer, WeeklyTrackingRecord> byWeek = new java.util.HashMap<>();
    private final List<WeeklyTrackingRecord> upserted = new ArrayList<>();
    private UUID lastOwnerId;

    @Override
    public List<WeeklyTrackingRecord> findAllByOwner(UUID userId) {
      lastOwnerId = userId;
      return List.copyOf(byWeek.values());
    }

    @Override
    public Optional<WeeklyTrackingRecord> findByOwnerAndWeek(UUID userId, int week) {
      lastOwnerId = userId;
      return Optional.ofNullable(byWeek.get(week));
    }

    @Override
    public WeeklyTrackingRecord upsert(UUID userId, WeeklyTrackingRecord record) {
      lastOwnerId = userId;
      upserted.add(record);
      byWeek.put(record.week(), record);
      return record;
    }
  }
}

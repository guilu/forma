package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.AdherenceCategory;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.CategoryAdherence;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link AdherenceService} (FOR-129, slice 2 of FOR-104): per
 * category planned/completed counting over a rolling window, asserted against hand-computed
 * fixtures (spec FOR-129 tests.md). Hand-rolled in-memory fakes (no Spring, no Mockito), matching
 * {@code MealLogServiceTest} (FOR-127) / {@code WeeklyTrainingScheduleServiceTest} (FOR-26).
 *
 * <p>{@code TODAY} (2026-07-15) is a Wednesday -&gt; RUNNING day per the shared weekly training day
 * policy (FOR-151: Diego's real plan; mirrors {@code MealLogServiceTest}'s fixed clock), so the
 * 7-day window {@code [2026-07-09, 2026-07-15]} covers, by weekday: Thu(STRENGTH) Fri(REST)
 * Sat(RUNNING) Sun(STRENGTH) Mon(RUNNING) Tue(STRENGTH) Wed(RUNNING) -&gt; 3 RUNNING days + 3
 * STRENGTH days = 6 planned sessions (one entry per non-rest day), 1 REST day.
 */
class AdherenceServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

  private final FakeStatusRepository statusRepository = new FakeStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), statusRepository);
  private final FakeMealLogRepository mealLogRepository = new FakeMealLogRepository();
  private final FakeBodyMeasurementRepository bodyMeasurementRepository =
      new FakeBodyMeasurementRepository();
  private final AdherenceService service =
      new AdherenceService(
          scheduleService, mealLogRepository, bodyMeasurementRepository, FIXED_CLOCK);

  @Test
  void windowSpansTodayMinusDaysPlusOneThroughToday() {
    Adherence adherence = service.compute(7);

    assertThat(adherence.windowDays()).isEqualTo(7);
    assertThat(adherence.from()).isEqualTo(LocalDate.of(2026, 7, 9));
    assertThat(adherence.to()).isEqualTo(TODAY);
  }

  @Test
  void
      trainingCountsPlannedFromTheScheduleAndCompletedFromStoredStatusesProjectedAcrossTheWindow() {
    // Only Saturday's running session and Tuesday's strength session are marked COMPLETED; that
    // status is the *current* per-weekday snapshot (FOR-27 has no per-date history), so it is
    // projected onto every occurrence of that weekday in the window (documented in
    // AdherenceService).
    statusRepository.upsert("SATURDAY:RUNNING", SessionStatus.COMPLETED, null);
    statusRepository.upsert("TUESDAY:STRENGTH", SessionStatus.COMPLETED, null);

    Adherence adherence = service.compute(7);

    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.planned()).isEqualTo(6); // 3 RUNNING + 3 STRENGTH days, 1 REST day excluded
    assertThat(training.completed()).isEqualTo(2); // one Saturday + one Tuesday in the window
    assertThat(training.rate()).isEqualTo(2.0 / 6.0);
  }

  @Test
  void nutritionCompletedIsDaysWithAtLeastOneLoggedEntryPlannedIsDaysInWindow() {
    log(mealLogRepository, AdherenceService.OWNER_ID, LocalDate.of(2026, 7, 9));
    log(mealLogRepository, AdherenceService.OWNER_ID, LocalDate.of(2026, 7, 11));
    log(mealLogRepository, AdherenceService.OWNER_ID, LocalDate.of(2026, 7, 15));
    // Outside the window -- must not be counted.
    log(mealLogRepository, AdherenceService.OWNER_ID, LocalDate.of(2026, 7, 1));

    Adherence adherence = service.compute(7);

    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.planned()).isEqualTo(7);
    assertThat(nutrition.completed()).isEqualTo(3);
    assertThat(nutrition.rate()).isEqualTo(3.0 / 7.0);
  }

  @Test
  void nutritionOnlyCountsTheOwnersLoggedDays() {
    log(mealLogRepository, "other-owner", LocalDate.of(2026, 7, 9));

    Adherence adherence = service.compute(7);

    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.completed()).isZero();
  }

  @Test
  void measurementsCompletedIsActualEntriesInWindowPlannedIsWeeklyCadenceCeiling() {
    measure(bodyMeasurementRepository, Instant.parse("2026-07-10T08:00:00Z"));
    measure(bodyMeasurementRepository, Instant.parse("2026-07-13T08:00:00Z"));
    // Outside the window -- must not be counted.
    measure(bodyMeasurementRepository, Instant.parse("2026-07-01T08:00:00Z"));

    Adherence adherence = service.compute(7);

    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.planned()).isEqualTo(1); // ceil(7/7) = 1 expected weekly measurement
    assertThat(measurements.completed()).isEqualTo(2); // completed > planned, allowed
    assertThat(measurements.rate()).isEqualTo(1.0); // capped, never > 1.0
  }

  @Test
  void aOneDayWindowOnARestDayHasZeroPlannedTrainingAndANullRate() {
    // 2026-07-17 is a Friday -> REST day (no running/strength scheduled) under FOR-151's mapping.
    Clock fridayClock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);
    AdherenceService fridayService =
        new AdherenceService(
            scheduleService, mealLogRepository, bodyMeasurementRepository, fridayClock);

    Adherence adherence = fridayService.compute(1);

    assertThat(adherence.from()).isEqualTo(adherence.to());
    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.planned()).isZero();
    assertThat(training.completed()).isZero();
    assertThat(training.rate()).isNull();
  }

  @Test
  void emptyDataReturnsZeroedCategoriesNeverAnError() {
    Adherence adherence = service.compute(7);

    assertThat(adherence.categories()).hasSize(3);
    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.completed()).isZero();
    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.completed()).isZero();
  }

  @Test
  void rejectsDaysBelowTheMinimum() {
    assertThatThrownBy(() -> service.compute(0)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsDaysAboveTheMaximum() {
    assertThatThrownBy(() -> service.compute(366)).isInstanceOf(ValidationException.class);
  }

  private static CategoryAdherence byCategory(Adherence adherence, AdherenceCategory category) {
    return adherence.categories().stream()
        .filter(c -> c.category() == category)
        .findFirst()
        .orElseThrow();
  }

  private static void log(FakeMealLogRepository repository, String ownerId, LocalDate date) {
    repository.save(
        ownerId,
        MealLogEntry.freeEntry(
            date, MealType.LUNCH, "X", new NutritionTotals(100, 10.0, 10.0, 10.0)));
  }

  private static void measure(FakeBodyMeasurementRepository repository, Instant measuredAt) {
    repository.save(
        new BodyMeasurement(
            measuredAt, MeasurementSource.MANUAL, 80.0, null, null, null, null, null));
  }

  /**
   * In-memory {@link TrainingSessionStatusRepository}, matching {@code
   * WeeklyTrainingScheduleServiceTest}.
   */
  private static final class FakeStatusRepository implements TrainingSessionStatusRepository {
    private final Map<String, StoredSessionStatus> stored = new HashMap<>();

    @Override
    public Map<String, StoredSessionStatus> findAll() {
      return stored;
    }

    @Override
    public void upsert(String sessionId, SessionStatus status, String notes) {
      stored.put(sessionId, new StoredSessionStatus(sessionId, status, notes));
    }
  }

  /** In-memory {@link MealLogRepository}, matching {@code MealLogServiceTest}'s fake shape. */
  private static final class FakeMealLogRepository implements MealLogRepository {
    private final List<OwnedEntry> rows = new ArrayList<>();

    @Override
    public List<StoredMealLogEntry> findByOwnerAndDate(String ownerId, LocalDate date) {
      return rows.stream()
          .filter(r -> r.ownerId.equals(ownerId) && r.stored.entry().date().equals(date))
          .map(r -> r.stored)
          .toList();
    }

    @Override
    public StoredMealLogEntry save(String ownerId, MealLogEntry entry) {
      StoredMealLogEntry stored = new StoredMealLogEntry(UUID.randomUUID().toString(), entry);
      rows.add(new OwnedEntry(ownerId, stored));
      return stored;
    }

    private record OwnedEntry(String ownerId, StoredMealLogEntry stored) {}
  }

  /** In-memory {@link BodyMeasurementRepository}. */
  private static final class FakeBodyMeasurementRepository implements BodyMeasurementRepository {
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

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
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

  /**
   * FOR-145b-2 security fix (🟠 MEDIUM cross-account signal leak): TRAINING/MEASUREMENTS are only
   * computed from the still-unscoped global tables for the seeded legacy placeholder account (see
   * {@code AdherenceService} javadoc's INTERIM security guard). Every TRAINING/MEASUREMENTS
   * assertion in this class therefore runs as the placeholder; {@link
   * #aNonPlaceholderUserGetsZeroedTrainingAndMeasurementsWithoutConsultingTheGlobalRepositories()}
   * proves the guard for a real, non-placeholder caller.
   */
  private static final UUID USER_ID = LegacyUserBootstrap.PLACEHOLDER_USER_ID;

  private final FakeStatusRepository statusRepository = new FakeStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(), new WorkoutTemplateService(), statusRepository);
  private final FakeMealLogRepository mealLogRepository = new FakeMealLogRepository();
  private final FakeBodyMeasurementRepository bodyMeasurementRepository =
      new FakeBodyMeasurementRepository();
  private final AdherenceService service =
      new AdherenceService(
          scheduleService,
          mealLogRepository,
          bodyMeasurementRepository,
          FIXED_CLOCK,
          () -> USER_ID);

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
    log(mealLogRepository, USER_ID, LocalDate.of(2026, 7, 9));
    log(mealLogRepository, USER_ID, LocalDate.of(2026, 7, 11));
    log(mealLogRepository, USER_ID, LocalDate.of(2026, 7, 15));
    // Outside the window -- must not be counted.
    log(mealLogRepository, USER_ID, LocalDate.of(2026, 7, 1));

    Adherence adherence = service.compute(7);

    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.planned()).isEqualTo(7);
    assertThat(nutrition.completed()).isEqualTo(3);
    assertThat(nutrition.rate()).isEqualTo(3.0 / 7.0);
  }

  @Test
  void nutritionOnlyCountsTheOwnersLoggedDays() {
    log(mealLogRepository, UUID.randomUUID(), LocalDate.of(2026, 7, 9));

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
            scheduleService,
            mealLogRepository,
            bodyMeasurementRepository,
            fridayClock,
            () -> USER_ID);

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

  /**
   * FOR-145b-2: real per-user wiring (the 145b-1 interim {@code requireLegacyOwner()} guard was
   * removed). A different authenticated user's {@code compute()} call returns 200 with THEIR OWN
   * (empty) NUTRITION data — never a 404, and never {@code USER_ID}'s logged days.
   */
  @Test
  void aDifferentAuthenticatedUserSeesTheirOwnEmptyNutritionDataNeverTheOtherUsers() {
    log(mealLogRepository, USER_ID, LocalDate.of(2026, 7, 9));
    UUID otherUserId = UUID.randomUUID();
    AdherenceService otherUserService =
        new AdherenceService(
            scheduleService,
            mealLogRepository,
            bodyMeasurementRepository,
            FIXED_CLOCK,
            () -> otherUserId);

    Adherence adherence = otherUserService.compute(7);

    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.completed()).isZero();
  }

  /**
   * FOR-145b-2 SECURITY FIX (🟠 MEDIUM cross-account signal leak, post-review): a real,
   * non-placeholder caller must never have their TRAINING/MEASUREMENTS numbers derived from the
   * still-unscoped global {@code training_session_status}/{@code body_measurements} tables (145c
   * gap). Both categories come back zeroed, and — critically — the global repositories are never
   * even consulted for that caller (asserted via call counters that would otherwise reflect the
   * seeded global data below). NUTRITION stays real, since {@link MealLogRepository} is properly
   * user-scoped.
   */
  @Test
  void
      aNonPlaceholderUserGetsZeroedTrainingAndMeasurementsWithoutConsultingTheGlobalRepositories() {
    // Seed the global (unscoped) tables with data that WOULD change the result if read.
    statusRepository.upsert("SATURDAY:RUNNING", SessionStatus.COMPLETED, null);
    measure(bodyMeasurementRepository, Instant.parse("2026-07-13T08:00:00Z"));
    UUID nonPlaceholderUserId = UUID.randomUUID();
    log(mealLogRepository, nonPlaceholderUserId, LocalDate.of(2026, 7, 11));
    int findAllCallsBefore = statusRepository.findAllCallCount;
    int listCallsBefore = bodyMeasurementRepository.listCallCount;
    AdherenceService nonPlaceholderService =
        new AdherenceService(
            scheduleService,
            mealLogRepository,
            bodyMeasurementRepository,
            FIXED_CLOCK,
            () -> nonPlaceholderUserId);

    Adherence adherence = nonPlaceholderService.compute(7);

    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.planned()).isZero();
    assertThat(training.completed()).isZero();
    assertThat(training.rate()).isNull();
    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.planned()).isZero();
    assertThat(measurements.completed()).isZero();
    assertThat(measurements.rate()).isNull();
    // The global repos were never consulted for this non-placeholder caller.
    assertThat(statusRepository.findAllCallCount).isEqualTo(findAllCallsBefore);
    assertThat(bodyMeasurementRepository.listCallCount).isEqualTo(listCallsBefore);
    // NUTRITION is unaffected -- still computed for real from this user's own meal_log rows.
    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.completed()).isEqualTo(1);
  }

  /**
   * Sanity counterpart to the guard above: the seeded legacy placeholder account keeps full,
   * unchanged TRAINING/MEASUREMENTS behavior (all other tests in this class already exercise this
   * via {@code USER_ID}, which is the placeholder — this test makes that guarantee explicit).
   */
  @Test
  void thePlaceholderAccountKeepsFullTrainingAndMeasurementsBehavior() {
    assertThat(USER_ID).isEqualTo(LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    statusRepository.upsert("SATURDAY:RUNNING", SessionStatus.COMPLETED, null);
    measure(bodyMeasurementRepository, Instant.parse("2026-07-13T08:00:00Z"));

    Adherence adherence = service.compute(7);

    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.completed()).isEqualTo(1);
    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.completed()).isEqualTo(1);
  }

  private static CategoryAdherence byCategory(Adherence adherence, AdherenceCategory category) {
    return adherence.categories().stream()
        .filter(c -> c.category() == category)
        .findFirst()
        .orElseThrow();
  }

  private static void log(FakeMealLogRepository repository, UUID userId, LocalDate date) {
    repository.save(
        userId,
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
    private int findAllCallCount = 0;

    @Override
    public Map<String, StoredSessionStatus> findAll() {
      findAllCallCount++;
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
    public List<StoredMealLogEntry> findByOwnerAndDate(UUID userId, LocalDate date) {
      return rows.stream()
          .filter(r -> r.userId.equals(userId) && r.stored.entry().date().equals(date))
          .map(r -> r.stored)
          .toList();
    }

    @Override
    public StoredMealLogEntry save(UUID userId, MealLogEntry entry) {
      StoredMealLogEntry stored = new StoredMealLogEntry(UUID.randomUUID().toString(), entry);
      rows.add(new OwnedEntry(userId, stored));
      return stored;
    }

    private record OwnedEntry(UUID userId, StoredMealLogEntry stored) {}
  }

  /** In-memory {@link BodyMeasurementRepository}. */
  private static final class FakeBodyMeasurementRepository implements BodyMeasurementRepository {
    private final List<BodyMeasurement> saved = new ArrayList<>();
    private int listCallCount = 0;

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      listCallCount++;
      return List.copyOf(saved);
    }
  }
}

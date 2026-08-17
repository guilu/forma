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
 *
 * <p>FOR-145c: {@link #statusRepository} and {@link #bodyMeasurementRepository} are per-user scoped
 * fakes (mirroring the real repos post migrations V30/V31), so every fixture in this class is
 * seeded against an explicit {@code userId} rather than a single implicit global table — the 145b-2
 * INTERIM security guard that used to zero TRAINING/MEASUREMENTS for non-placeholder callers was
 * removed by this slice (see {@code AdherenceService} javadoc).
 */
class AdherenceServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

  /** The Monday of {@link #TODAY}'s week — the week stored statuses are scoped to (V60). */
  private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

  private static final UUID USER_ID = UUID.randomUUID();

  private final FakeTrainingSessionStatusRepository statusRepository =
      new FakeTrainingSessionStatusRepository();
  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(),
          new WorkoutTemplateService(),
          statusRepository,
          () -> USER_ID,
          FIXED_CLOCK);
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
    statusRepository.upsertStatus(
        USER_ID, WEEK_START, "RUNNING:LONG_RUN", SessionStatus.COMPLETED, null, null);
    statusRepository.upsertStatus(
        USER_ID, WEEK_START, "STRENGTH:PUSH", SessionStatus.COMPLETED, null, null);

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
    measure(bodyMeasurementRepository, USER_ID, Instant.parse("2026-07-10T08:00:00Z"));
    measure(bodyMeasurementRepository, USER_ID, Instant.parse("2026-07-13T08:00:00Z"));
    // Outside the window -- must not be counted.
    measure(bodyMeasurementRepository, USER_ID, Instant.parse("2026-07-01T08:00:00Z"));

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
   * FOR-145c: {@code training_session_status} (V31) and {@code body_measurements} (V30) are now
   * {@code user_id}-scoped, so the 145b-2 INTERIM security guard is gone — a real, non-placeholder
   * caller gets TRAINING/MEASUREMENTS computed from THEIR OWN rows, never zeroed and never another
   * account's data.
   */
  @Test
  void aDifferentAuthenticatedUserGetsRealTrainingAndMeasurementsFromTheirOwnDataOnly() {
    // Seed USER_ID's data -- must NOT leak into the other user's numbers.
    statusRepository.upsertStatus(
        USER_ID, WEEK_START, "RUNNING:LONG_RUN", SessionStatus.COMPLETED, null, null);
    measure(bodyMeasurementRepository, USER_ID, Instant.parse("2026-07-13T08:00:00Z"));

    UUID otherUserId = UUID.randomUUID();
    statusRepository.upsertStatus(
        otherUserId, WEEK_START, "STRENGTH:PUSH", SessionStatus.COMPLETED, null, null);
    measure(bodyMeasurementRepository, otherUserId, Instant.parse("2026-07-11T08:00:00Z"));
    log(mealLogRepository, otherUserId, LocalDate.of(2026, 7, 11));

    WeeklyTrainingScheduleService otherScheduleService =
        new WeeklyTrainingScheduleService(
            new RunningPlanService(),
            new WorkoutTemplateService(),
            statusRepository,
            () -> otherUserId,
            FIXED_CLOCK);
    AdherenceService otherUserService =
        new AdherenceService(
            otherScheduleService,
            mealLogRepository,
            bodyMeasurementRepository,
            FIXED_CLOCK,
            () -> otherUserId);

    Adherence adherence = otherUserService.compute(7);

    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.planned()).isEqualTo(6);
    assertThat(training.completed()).isEqualTo(1); // only THEIR Tuesday, not USER_ID's Saturday
    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.completed()).isEqualTo(1); // only THEIR measurement
    CategoryAdherence nutrition = byCategory(adherence, AdherenceCategory.NUTRITION);
    assertThat(nutrition.completed()).isEqualTo(1);
  }

  /**
   * Sanity counterpart: {@code USER_ID}'s own TRAINING/MEASUREMENTS numbers are unaffected by
   * another user's data seeded into the same (now per-user scoped) repositories.
   */
  @Test
  void theOriginalUsersTrainingAndMeasurementsAreUnaffectedByAnotherUsersData() {
    statusRepository.upsertStatus(
        USER_ID, WEEK_START, "RUNNING:LONG_RUN", SessionStatus.COMPLETED, null, null);
    measure(bodyMeasurementRepository, USER_ID, Instant.parse("2026-07-13T08:00:00Z"));
    UUID otherUserId = UUID.randomUUID();
    statusRepository.upsertStatus(
        otherUserId, WEEK_START, "STRENGTH:PUSH", SessionStatus.COMPLETED, null, null);
    measure(bodyMeasurementRepository, otherUserId, Instant.parse("2026-07-12T08:00:00Z"));

    Adherence adherence = service.compute(7);

    CategoryAdherence training = byCategory(adherence, AdherenceCategory.TRAINING);
    assertThat(training.completed()).isEqualTo(1); // only USER_ID's Saturday
    CategoryAdherence measurements = byCategory(adherence, AdherenceCategory.MEASUREMENTS);
    assertThat(measurements.completed()).isEqualTo(1); // only USER_ID's measurement
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

  private static void measure(
      FakeBodyMeasurementRepository repository, UUID userId, Instant measuredAt) {
    repository.save(
        userId,
        new BodyMeasurement(
            measuredAt, MeasurementSource.MANUAL, 80.0, null, null, null, null, null));
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

    @Override
    public void deleteByOwnerDateAndPlannedMeal(UUID userId, LocalDate date, UUID plannedMealId) {
      rows.removeIf(
          row ->
              row.userId.equals(userId)
                  && row.stored.entry().date().equals(date)
                  && plannedMealId.equals(row.stored.entry().plannedMealId()));
    }

    private record OwnedEntry(UUID userId, StoredMealLogEntry stored) {}
  }

  /**
   * In-memory {@link BodyMeasurementRepository}, scoped per {@code userId} (mirroring the real repo
   * post migration V30).
   */
  private static final class FakeBodyMeasurementRepository implements BodyMeasurementRepository {
    private final Map<UUID, List<BodyMeasurement>> saved = new HashMap<>();

    @Override
    public void save(UUID userId, BodyMeasurement measurement) {
      saved.computeIfAbsent(userId, k -> new ArrayList<>()).add(measurement);
    }

    @Override
    public List<BodyMeasurement> list(UUID userId) {
      return List.copyOf(saved.getOrDefault(userId, List.of()));
    }

    /**
     * Unused here: this fake exists for the read paths ({@code list}). FOR-187's id-aware listing
     * and delete belong to the measurements use case, which has its own test.
     */
    @Override
    public java.util.List<StoredBodyMeasurement> listWithIds(UUID userId) {
      throw new UnsupportedOperationException("not used in this test");
    }

    @Override
    public boolean delete(UUID userId, UUID id) {
      throw new UnsupportedOperationException("not used in this test");
    }
  }
}

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link WeeklyHistoryService} (FOR-139, slice 3 of FOR-104): Monday
 * -start weekly buckets over a bounded window, asserted against hand-computed fixtures (spec
 * FOR-139 tests.md).
 */
class WeeklyHistoryServiceTest {

  // 2026-07-15 is a Wednesday; the current week starts Monday 2026-07-13.
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);

  private final FakeMealLogRepository mealLogRepository = new FakeMealLogRepository();
  private final WeeklyHistoryService service =
      new WeeklyHistoryService(mealLogRepository, FIXED_CLOCK, () -> LEGACY_OWNER_UUID);

  @Test
  void seriesEndsWithTheCurrentWeekMondayThroughSunday() {
    WeeklyHistory history = service.compute(2);

    assertThat(history.weeks()).hasSize(2);
    assertThat(history.weeks().get(0).weekStart()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(history.weeks().get(1).weekStart()).isEqualTo(LocalDate.of(2026, 7, 13));
  }

  @Test
  void eachBucketHasSevenPlannedAndCompletedFromDistinctLoggedDaysInTheWeek() {
    log(LocalDate.of(2026, 7, 13)); // Monday of current week
    log(LocalDate.of(2026, 7, 14)); // Tuesday of current week
    log(LocalDate.of(2026, 7, 14)); // duplicate log same day -- still counts once
    log(LocalDate.of(2026, 7, 6)); // Monday of previous week

    WeeklyHistory history = service.compute(2);

    WeeklyHistoryBucket previousWeek = history.weeks().get(0);
    assertThat(previousWeek.planned()).isEqualTo(7);
    assertThat(previousWeek.completed()).isEqualTo(1);

    WeeklyHistoryBucket currentWeek = history.weeks().get(1);
    assertThat(currentWeek.planned()).isEqualTo(7);
    assertThat(currentWeek.completed()).isEqualTo(2);
  }

  @Test
  void weeksWithNoDataAreZeroBucketsStillPresentInTheSeries() {
    WeeklyHistory history = service.compute(3);

    assertThat(history.weeks()).hasSize(3);
    assertThat(history.weeks()).allSatisfy(week -> assertThat(week.completed()).isZero());
  }

  @Test
  void onlyCountsTheOwnersLoggedDays() {
    mealLogRepository.save(
        UUID.randomUUID(),
        MealLogEntry.freeEntry(
            LocalDate.of(2026, 7, 13),
            MealType.LUNCH,
            "X",
            new NutritionTotals(100, 10.0, 10.0, 10.0)));

    WeeklyHistory history = service.compute(1);

    assertThat(history.weeks().get(0).completed()).isZero();
  }

  @Test
  void rejectsWeeksBelowTheMinimum() {
    assertThatThrownBy(() -> service.compute(0)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsWeeksAboveTheMaximum() {
    assertThatThrownBy(() -> service.compute(53)).isInstanceOf(ValidationException.class);
  }

  @Test
  void aNonPlaceholderAuthenticatedCallerGets404NeverTheLegacyOwnersWeeklyHistory() {
    WeeklyHistoryService otherUserService =
        new WeeklyHistoryService(mealLogRepository, FIXED_CLOCK, UUID::randomUUID);

    assertThatThrownBy(() -> otherUserService.compute(8)).isInstanceOf(NotFoundException.class);
  }

  // FOR-145b-1: matches WeeklyHistoryService's internal LEGACY_OWNER_UUID compile-compat shim (the
  // UUID equivalent of the legacy OWNER_ID = "default-user" string).
  private static final UUID LEGACY_OWNER_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  private void log(LocalDate date) {
    mealLogRepository.save(
        LEGACY_OWNER_UUID,
        MealLogEntry.freeEntry(
            date, MealType.LUNCH, "X", new NutritionTotals(100, 10.0, 10.0, 10.0)));
  }

  /** In-memory {@link MealLogRepository}, matching {@code AdherenceServiceTest}'s fake shape. */
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
}

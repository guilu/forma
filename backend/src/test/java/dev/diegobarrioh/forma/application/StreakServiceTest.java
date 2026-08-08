package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.Streak;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link StreakService} (FOR-139, slice 3 of FOR-104): the
 * nutrition-per-date derivation and the bounded {@code days} window, asserted against hand-computed
 * fixtures (spec FOR-139 tests.md). Hand-rolled in-memory fake, matching {@code
 * AdherenceServiceTest}.
 */
class StreakServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

  private static final UUID USER_ID = UUID.randomUUID();

  private final FakeMealLogRepository mealLogRepository = new FakeMealLogRepository();
  private final StreakService service =
      new StreakService(mealLogRepository, FIXED_CLOCK, () -> USER_ID);

  @Test
  void currentStreakCountsConsecutiveLoggedDaysEndingToday() {
    log(LocalDate.of(2026, 7, 13));
    log(LocalDate.of(2026, 7, 14));
    log(TODAY);

    Streak streak = service.compute(30);

    assertThat(streak.currentStreakDays()).isEqualTo(3);
    assertThat(streak.asOf()).isEqualTo(TODAY);
  }

  @Test
  void aGapDayResetsTheCurrentStreak() {
    log(LocalDate.of(2026, 7, 10));
    log(LocalDate.of(2026, 7, 11));
    // Gap on 07-12..07-14.
    log(TODAY);

    Streak streak = service.compute(30);

    assertThat(streak.currentStreakDays()).isEqualTo(1);
  }

  @Test
  void longestStreakTracksTheLongestRunInTheWindowRegardlessOfTheCurrentRun() {
    log(LocalDate.of(2026, 7, 1));
    log(LocalDate.of(2026, 7, 2));
    log(LocalDate.of(2026, 7, 3));
    log(LocalDate.of(2026, 7, 4));
    log(TODAY);

    Streak streak = service.compute(30);

    assertThat(streak.currentStreakDays()).isEqualTo(1);
    assertThat(streak.longestStreakDays()).isEqualTo(4);
  }

  @Test
  void emptyHistoryReturnsAZeroedStreakNotAnError() {
    Streak streak = service.compute(30);

    assertThat(streak.currentStreakDays()).isZero();
    assertThat(streak.longestStreakDays()).isZero();
  }

  @Test
  void onlyCountsTheOwnersLoggedDays() {
    logForOwner(UUID.randomUUID(), TODAY);

    Streak streak = service.compute(30);

    assertThat(streak.currentStreakDays()).isZero();
  }

  @Test
  void aDayOutsideTheWindowDoesNotContributeToTheLongestStreak() {
    log(LocalDate.of(2026, 5, 1)); // far outside a 30-day window
    log(TODAY);

    Streak streak = service.compute(30);

    assertThat(streak.longestStreakDays()).isEqualTo(1);
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
   * (empty) streak — never a 404, and never {@code USER_ID}'s logged days.
   */
  @Test
  void aDifferentAuthenticatedUserSeesTheirOwnEmptyStreakNeverTheOtherUsers() {
    log(TODAY);
    StreakService otherUserService =
        new StreakService(mealLogRepository, FIXED_CLOCK, UUID::randomUUID);

    Streak streak = otherUserService.compute(30);

    assertThat(streak.currentStreakDays()).isZero();
  }

  private void log(LocalDate date) {
    logForOwner(USER_ID, date);
  }

  private void logForOwner(UUID userId, LocalDate date) {
    mealLogRepository.save(
        userId,
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
}

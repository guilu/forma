package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application use case for the FOR-139 weekly-history read model (slice 3 of FOR-104, unblocks
 * FOR-53's weekly-history bars): a bounded, per-week planned-vs-completed series — no new
 * persistence (spec FOR-139 NFR "no migration"; head stays V18).
 *
 * <p><b>Signal (documented, resolves spec FOR-139 Open Question):</b> each bucket uses the same
 * nutrition signal as {@link StreakService} — {@code completed} is the count of distinct days in
 * the week with at least one logged meal ({@link MealLogRepository#findByOwnerAndDate}); {@code
 * planned} is 7 (days in the week), mirroring {@link AdherenceService}'s NUTRITION category
 * ("planned" = "days in window", spec FOR-129). <b>A training bucket is deliberately NOT included
 * in this slice</b>: {@code training_session_status} has no per-date history (documented in {@link
 * AdherenceService}), so a per-week training bar would either fabricate history or silently repeat
 * the same current-week-projected pattern for every week in the series — neither is honest (spec
 * FOR-139: "do NOT fabricate per-date training completion"). Showing a real per-date training bar
 * is follow-up work once per-date training completion history exists.
 *
 * <p>Weeks start on Monday, mirroring {@code WeeklyCheckInService}'s week-start convention.
 *
 * <p><b>Bounded window:</b> {@code weeks} is bounded to {@code [1, 52]} — 52 weeks * 7 days = 364
 * days, just under {@link StreakService#MAX_DAYS}'s 365-day precedent — bounded per-request
 * computation acceptable at MVP volume (spec FOR-139 NFR "Performance"). The default of 8 weeks
 * (documented per spec FOR-139 api.md, within the spec's suggested 8-12 range) covers roughly two
 * months of bars.
 *
 * <p>Real multi-user auth (FOR-145b-2, ADR-012): resolves the caller's account id via {@link
 * CurrentUserProvider} and passes it to {@link MealLogRepository} — replacing the old fixed {@code
 * OWNER_ID = "default-user"} constant and the 145b-1 interim {@code requireLegacyOwner()} guard
 * (both removed by this slice).
 */
@Service
public class WeeklyHistoryService {

  /** Bounded {@code weeks} range: outside this, the request is rejected. */
  static final int MIN_WEEKS = 1;

  static final int MAX_WEEKS = 52;

  private final MealLogRepository mealLogRepository;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public WeeklyHistoryService(
      MealLogRepository mealLogRepository, Clock clock, CurrentUserProvider currentUserProvider) {
    this.mealLogRepository = mealLogRepository;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Computes the last {@code weeks} weekly buckets, ordered oldest-first, ending with the current
   * week (the Monday-through-Sunday week containing today).
   *
   * @throws ValidationException if {@code weeks} is outside {@code [1, 52]}
   */
  public WeeklyHistory compute(int weeks) {
    if (weeks < MIN_WEEKS || weeks > MAX_WEEKS) {
      throw new ValidationException(
          "weeks must be between " + MIN_WEEKS + " and " + MAX_WEEKS + ", was: " + weeks);
    }
    UUID userId = currentUserProvider.currentUserId();

    LocalDate currentWeekStart =
        LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate firstWeekStart = currentWeekStart.minusWeeks(weeks - 1L);

    List<WeeklyHistoryBucket> buckets = new ArrayList<>();
    for (LocalDate weekStart = firstWeekStart;
        !weekStart.isAfter(currentWeekStart);
        weekStart = weekStart.plusWeeks(1)) {
      buckets.add(new WeeklyHistoryBucket(weekStart, 7, completedDaysIn(userId, weekStart)));
    }

    return new WeeklyHistory(buckets);
  }

  private int completedDaysIn(UUID userId, LocalDate weekStart) {
    LocalDate weekEnd = weekStart.plusDays(6);
    int completed = 0;
    for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
      if (!mealLogRepository.findByOwnerAndDate(userId, date).isEmpty()) {
        completed++;
      }
    }
    return completed;
  }
}

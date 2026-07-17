package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
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
 */
@Service
public class WeeklyHistoryService {

  /**
   * Fixed single-user owner id for the MVP (ADR-002), mirroring {@link AdherenceService#OWNER_ID}.
   * Duplicated here rather than introduced as a shared abstraction, matching {@link
   * StreakService#OWNER_ID}.
   */
  public static final String OWNER_ID = "default-user";

  /** Bounded {@code weeks} range: outside this, the request is rejected. */
  static final int MIN_WEEKS = 1;

  static final int MAX_WEEKS = 52;

  private final MealLogRepository mealLogRepository;
  private final Clock clock;

  public WeeklyHistoryService(MealLogRepository mealLogRepository, Clock clock) {
    this.mealLogRepository = mealLogRepository;
    this.clock = clock;
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

    LocalDate currentWeekStart =
        LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate firstWeekStart = currentWeekStart.minusWeeks(weeks - 1L);

    List<WeeklyHistoryBucket> buckets = new ArrayList<>();
    for (LocalDate weekStart = firstWeekStart;
        !weekStart.isAfter(currentWeekStart);
        weekStart = weekStart.plusWeeks(1)) {
      buckets.add(new WeeklyHistoryBucket(weekStart, 7, completedDaysIn(weekStart)));
    }

    return new WeeklyHistory(buckets);
  }

  private int completedDaysIn(LocalDate weekStart) {
    LocalDate weekEnd = weekStart.plusDays(6);
    int completed = 0;
    for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
      if (!mealLogRepository.findByOwnerAndDate(OWNER_ID, date).isEmpty()) {
        completed++;
      }
    }
    return completed;
  }
}

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
 */
@Service
public class WeeklyHistoryService {

  /**
   * Fixed single-user owner id for the MVP (ADR-002), mirroring {@link AdherenceService#OWNER_ID}.
   * Duplicated here rather than introduced as a shared abstraction, matching {@link
   * StreakService#OWNER_ID}.
   */
  public static final String OWNER_ID = "default-user";

  /**
   * FOR-145b-1 compile-compat shim: {@link MealLogRepository} (Class A, migration V27) now takes a
   * real {@code UUID}. {@code WeeklyHistoryService} itself stays on the legacy String {@link
   * #OWNER_ID} for now (deferred to 145b-2) — this constant is ONLY the UUID equivalent of that
   * same legacy owner, used solely for the {@link #mealLogRepository} call below. Not a behavior
   * change.
   */
  private static final UUID LEGACY_OWNER_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

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
   * @throws NotFoundException if the caller is not the legacy placeholder account (interim security
   *     guard, mandatory review of 145b-1, HIGH cross-account disclosure — see {@link
   *     #requireLegacyOwner()})
   */
  public WeeklyHistory compute(int weeks) {
    requireLegacyOwner();
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
      if (!mealLogRepository.findByOwnerAndDate(LEGACY_OWNER_UUID, date).isEmpty()) {
        completed++;
      }
    }
    return completed;
  }

  /**
   * Interim security guard (mandatory review of 145b-1, HIGH cross-account disclosure): this
   * service still reads only the legacy placeholder owner's nutrition history ({@link
   * #LEGACY_OWNER_UUID}). Until 145b-2 wires a real per-user owner here, any authenticated caller
   * other than the placeholder account must get a 404, never the legacy owner's weekly history.
   *
   * @throws NotFoundException if the caller is not the legacy placeholder account
   */
  private void requireLegacyOwner() {
    if (!currentUserProvider.currentUserId().equals(LEGACY_OWNER_UUID)) {
      throw new NotFoundException("No existen datos de progreso para este usuario");
    }
  }
}

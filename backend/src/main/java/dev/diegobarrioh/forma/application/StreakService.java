package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Streak;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Application use case for the FOR-139 streak read model (slice 3 of FOR-104, unblocks FOR-53's
 * "RACHA ACTUAL"): current + longest consistency streak, derived on demand from real per-date
 * nutrition history — no new persistence (spec FOR-139 NFR "no migration"; head stays V18).
 *
 * <p><b>Qualifying activity (documented, resolves spec FOR-139 Open Question):</b> a day is
 * "consistent" when the owner logged at least one nutrition meal-log entry for that date ({@link
 * MealLogRepository#findByOwnerAndDate}) — the strongest real per-date, owner-scoped fact available
 * today. Measurements/water intake are deliberately NOT folded in: they follow a different, sparser
 * cadence, and mixing signals with different natural rhythms into one streak would make the rule
 * harder to audit, not easier (spec FOR-139 NFR "Explainability"). {@code training_session_status}
 * (FOR-27, migration V3) is deliberately NOT used either: it stores only the *current* status per
 * weekday slot, with no per-date completion history (documented in {@link AdherenceService}), so it
 * cannot answer "was <i>this specific past date</i> consistent" — using it here would fabricate
 * history the repository does not have (AGENTS.md: "do not invent requirements").
 *
 * <p>The pure streak algorithm (gap reset, today-inclusivity, longest-run tracking) lives in {@link
 * Streak#of} (ADR-001: domain stays framework-free); this service is only responsible for deciding
 * which dates qualify and fetching them — no duplicated policy logic.
 *
 * <p><b>Bounded window:</b> the {@code days}-long lookback bounds both the nutrition query (looped
 * per date across {@link MealLogRepository}, mirroring {@link AdherenceService}'s nutrition
 * derivation — no by-date-range query port exists on that repository) and the horizon within which
 * {@code longestStreakDays} is computed; a run that started before the window is truncated at the
 * window boundary. Bounded to {@code [1, 365]}, mirroring {@link AdherenceService}'s bound —
 * bounded per-request computation acceptable at MVP volume (spec FOR-139 NFR "Performance"). The
 * default of 90 days (documented per spec FOR-139 api.md) covers a typical "last few months" streak
 * horizon without scanning the owner's entire history on every request.
 */
@Service
public class StreakService {

  /**
   * Fixed single-user owner id for the MVP (ADR-002), mirroring {@link AdherenceService#OWNER_ID}.
   * Duplicated here rather than introduced as a shared abstraction — see {@code GoalService}'s
   * javadoc for the rationale (no speculative abstraction beyond scope).
   */
  public static final String OWNER_ID = "default-user";

  /** Bounded {@code days} range: outside this, the request is rejected. */
  static final int MIN_DAYS = 1;

  static final int MAX_DAYS = 365;

  private final MealLogRepository mealLogRepository;
  private final Clock clock;

  public StreakService(MealLogRepository mealLogRepository, Clock clock) {
    this.mealLogRepository = mealLogRepository;
    this.clock = clock;
  }

  /**
   * Computes the streak over a {@code days}-long lookback window ending today (inclusive on both
   * ends: {@code [today - days + 1, today]}).
   *
   * @throws ValidationException if {@code days} is outside {@code [1, 365]}
   */
  public Streak compute(int days) {
    if (days < MIN_DAYS || days > MAX_DAYS) {
      throw new ValidationException(
          "days must be between " + MIN_DAYS + " and " + MAX_DAYS + ", was: " + days);
    }

    LocalDate asOf = LocalDate.now(clock);
    LocalDate windowStart = asOf.minusDays(days - 1L);

    Set<LocalDate> activeDates = new HashSet<>();
    for (LocalDate date = windowStart; !date.isAfter(asOf); date = date.plusDays(1)) {
      if (!mealLogRepository.findByOwnerAndDate(OWNER_ID, date).isEmpty()) {
        activeDates.add(date);
      }
    }

    return Streak.of(activeDates, windowStart, asOf);
  }
}

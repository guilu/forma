package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * Consistency-streak read model (FOR-139, slice 3 of FOR-104): current and longest run of
 * consecutive "consistent days" ending at {@code asOf}, computed purely from a caller-supplied set
 * of qualifying dates (ADR-001: the domain stays framework-free; the application layer decides
 * *which* dates qualify and fetches them from the relevant repositories — see {@code
 * StreakService}).
 *
 * <h2>Streak rule (documented, auditable — spec FOR-139)</h2>
 *
 * <ul>
 *   <li><b>Consistent day</b>: any date present in the {@code activeDates} set passed to {@link
 *       #of}. FOR-139 defines that as a day with at least one logged nutrition meal-log entry — the
 *       strongest real per-date, owner-scoped fact available (documented in {@code StreakService},
 *       since {@code training_session_status} has no per-date history).
 *   <li><b>{@code currentStreakDays}</b>: the number of consecutive consistent days walking
 *       backward from {@code asOf}, stopping at the first date that is either not in {@code
 *       activeDates} or before {@code windowStart}. <b>Today-inclusivity is strict</b>: if {@code
 *       asOf} itself is not a consistent day, {@code currentStreakDays} is {@code 0} — there is no
 *       end-of-day grace period, because the backend has no signal that more activity might still
 *       be logged later today. This keeps the rule simple and auditable from the stored dates alone
 *       (spec FOR-139: "pick one and document it").
 *   <li><b>Gap day</b>: any date not in {@code activeDates} breaks the current run; a later
 *       consistent day starts a new run at 1 — it does not resume the old count.
 *   <li><b>{@code longestStreakDays}</b>: the longest run of consecutive consistent days found
 *       anywhere in the closed window {@code [windowStart, asOf]}, independent of the current run
 *       (a gap right before {@code asOf} does not erase a longer run earlier in the window).
 * </ul>
 *
 * <p>Empty {@code activeDates} (no history at all) yields {@code currentStreakDays == 0} and {@code
 * longestStreakDays == 0} — never an error (spec FOR-139 Edge Cases).
 *
 * @param currentStreakDays consecutive consistent days ending at {@code asOf}; never negative
 * @param longestStreakDays the longest consecutive run within the window; never negative, always
 *     &ge; {@code currentStreakDays}
 * @param asOf the date the streak was computed as of (owner timezone, resolved by the caller's
 *     {@link java.time.Clock})
 */
public record Streak(int currentStreakDays, int longestStreakDays, LocalDate asOf) {

  /**
   * Computes the streak from {@code activeDates} over the closed window {@code [windowStart,
   * asOf]}.
   *
   * @param activeDates the dates that qualify as "consistent"; only entries within {@code
   *     [windowStart, asOf]} affect the result
   * @param windowStart the first (inclusive) date considered — bounds {@code longestStreakDays};
   *     must not be after {@code asOf}
   * @param asOf the date to compute the streak as of; the last (inclusive) date considered
   * @throws IllegalArgumentException if {@code windowStart} is after {@code asOf}
   */
  public static Streak of(Set<LocalDate> activeDates, LocalDate windowStart, LocalDate asOf) {
    if (windowStart.isAfter(asOf)) {
      throw new IllegalArgumentException(
          "windowStart must not be after asOf: " + windowStart + " > " + asOf);
    }

    int current = 0;
    for (LocalDate date = asOf;
        !date.isBefore(windowStart) && activeDates.contains(date);
        date = date.minusDays(1)) {
      current++;
    }

    int longest = 0;
    int run = 0;
    for (LocalDate date = windowStart; !date.isAfter(asOf); date = date.plusDays(1)) {
      if (activeDates.contains(date)) {
        run++;
        longest = Math.max(longest, run);
      } else {
        run = 0;
      }
    }

    return new Streak(current, longest, asOf);
  }
}

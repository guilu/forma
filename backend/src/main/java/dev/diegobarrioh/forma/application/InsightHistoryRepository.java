package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting each generated {@link WeeklyInsights} keyed by its period (FOR-110). Owned by
 * the application side; adapters implement it (ADR-001). Persistence sits behind the existing
 * {@link WeeklyInsightsService} generation path — this port never re-runs the FOR-42/43/44
 * recommendation rules, it only stores/retrieves what was already produced.
 */
public interface InsightHistoryRepository {

  /**
   * Upserts the given insights keyed by {@code insights.checkIn().weekStartDate()}. Re-running
   * generation for a period that was already stored replaces its row and recommendations in place
   * (spec FOR-110 Edge Cases: repeated generation within the same period overwrites, it does not
   * append a duplicate).
   *
   * @param insights the freshly generated insights to persist; must not be {@code null}
   */
  void save(WeeklyInsights insights);

  /**
   * Returns every persisted period's insights, most recent period first.
   *
   * @return the persisted history, or an empty list when nothing has been generated yet (spec
   *     FOR-110 Edge Cases: not an error)
   */
  List<WeeklyInsights> listAll();

  /**
   * Finds the persisted check-in snapshot for the most recent period strictly before {@code
   * period}, used to compute week-over-week deltas. A gap week with no persisted period compares
   * against the most recent prior persisted period, not a fabricated intermediate one (spec FOR-110
   * Edge Cases).
   *
   * @param period the period to look strictly before
   * @return the prior period's check-in, or empty when {@code period} is the first-ever persisted
   *     period (or none exists yet)
   */
  Optional<WeeklyCheckIn> findMostRecentCheckInBefore(LocalDate period);
}

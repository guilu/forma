package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting each generated {@link WeeklyInsights} keyed by its period (FOR-110). Owned by
 * the application side; adapters implement it (ADR-001). Persistence sits behind the existing
 * {@link WeeklyInsightsService} generation path — this port never re-runs the FOR-42/43/44
 * recommendation rules, it only stores/retrieves what was already produced.
 *
 * <p>{@code userId} is a real account id (FOR-145c "gap table" closure, migration V34) — {@code
 * insight_history}'s primary key was rebuilt from the bare {@code week_start_date} (a period key
 * shared by every user — a genuine cross-user collision bug) to the composite {@code (user_id,
 * week_start_date)}; its child {@code insight_history_recommendation} was rebuilt in lockstep.
 * Before this slice the tables had NO owner-scoping at all.
 */
public interface InsightHistoryRepository {

  /**
   * Upserts the given insights for {@code userId}, keyed by {@code
   * insights.checkIn().weekStartDate()}. Re-running generation for a period that was already stored
   * replaces its row and recommendations in place (spec FOR-110 Edge Cases: repeated generation
   * within the same period overwrites, it does not append a duplicate).
   *
   * @param userId the owning account's id
   * @param insights the freshly generated insights to persist; must not be {@code null}
   */
  void save(UUID userId, WeeklyInsights insights);

  /**
   * Returns every one of {@code userId}'s persisted period insights, most recent period first.
   *
   * @param userId the owning account's id
   * @return the persisted history, or an empty list when nothing has been generated yet (spec
   *     FOR-110 Edge Cases: not an error)
   */
  List<WeeklyInsights> listAll(UUID userId);

  /**
   * Finds {@code userId}'s persisted check-in snapshot for the most recent period strictly before
   * {@code period}, used to compute week-over-week deltas. A gap week with no persisted period
   * compares against the most recent prior persisted period, not a fabricated intermediate one
   * (spec FOR-110 Edge Cases).
   *
   * @param userId the owning account's id
   * @param period the period to look strictly before
   * @return the prior period's check-in, or empty when {@code period} is the first-ever persisted
   *     period (or none exists yet)
   */
  Optional<WeeklyCheckIn> findMostRecentCheckInBefore(UUID userId, LocalDate period);
}

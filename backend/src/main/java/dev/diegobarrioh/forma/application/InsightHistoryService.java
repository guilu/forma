package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import dev.diegobarrioh.forma.domain.WeeklyCheckInDeltas;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case for the FOR-110 insight history read side: listing past persisted periods
 * and computing week-over-week deltas against the immediately prior persisted period.
 *
 * <p>Reads only from the {@link InsightHistoryRepository} persisted store — it never re-runs the
 * FOR-42/43/44 recommendation rules retroactively (ai-context.md Architectural Constraints).
 * Persisting on generation is {@link WeeklyInsightsService}'s responsibility, not this service's.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V34): this "gap table" service had ZERO
 * owner-scoping before this slice. Resolves the caller's account id via {@link CurrentUserProvider}
 * and passes it to the repository on every call.
 */
@Service
public class InsightHistoryService {

  private final InsightHistoryRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public InsightHistoryService(
      InsightHistoryRepository repository, CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Returns every persisted period's insights for the caller, most recent first; empty before any
   * exist.
   */
  public List<WeeklyInsights> history() {
    return repository.listAll(currentUserProvider.currentUserId());
  }

  /**
   * Computes {@code checkIn}'s week-over-week deltas against the caller's most recent persisted
   * period strictly before it. Returns {@link WeeklyCheckInDeltas#NONE} (all fields null) when
   * there is no prior period, never fabricated zeros.
   */
  public WeeklyCheckInDeltas deltasFor(WeeklyCheckIn checkIn) {
    return repository
        .findMostRecentCheckInBefore(currentUserProvider.currentUserId(), checkIn.weekStartDate())
        .map(prior -> WeeklyCheckInDeltas.between(checkIn, prior))
        .orElse(WeeklyCheckInDeltas.NONE);
  }
}

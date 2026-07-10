package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that assembles the weekly insights (FOR-45): builds the FOR-40 {@link
 * WeeklyCheckIn}, runs the FOR-42/43/44 rule sets, selects a prioritized main recommendation and
 * returns them with a generated timestamp. Computed on demand — no persisted insights, consistent
 * with the FOR-21/FOR-28 services.
 *
 * <h2>Main-recommendation selection (documented per spec FOR-45 Open Questions)</h2>
 *
 * Recommendations are ordered by <b>severity priority</b> {@code ACTION > WARNING > INFO}; the
 * first is the {@code main} one and the rest are the secondaries. The sort is <b>stable</b>, so
 * ties keep their production order — body (FOR-42), then training (FOR-43), then recovery (FOR-44).
 * Empty data still yields recommendations (each of the body/training rules always emits one, e.g.
 * an insufficient-data {@code INFO}), so {@code main} is never null and the endpoint never errors
 * on absent data.
 */
@Service
public class WeeklyInsightsService {

  private final WeeklyCheckInService checkInService;
  private final BodyTrendRecommendationService bodyTrendService;
  private final TrainingAdherenceRecommendationService trainingAdherenceService;
  private final RecoveryWarningRecommendationService recoveryWarningService;
  private final Clock clock;

  public WeeklyInsightsService(
      WeeklyCheckInService checkInService,
      BodyTrendRecommendationService bodyTrendService,
      TrainingAdherenceRecommendationService trainingAdherenceService,
      RecoveryWarningRecommendationService recoveryWarningService,
      Clock clock) {
    this.checkInService = checkInService;
    this.bodyTrendService = bodyTrendService;
    this.trainingAdherenceService = trainingAdherenceService;
    this.recoveryWarningService = recoveryWarningService;
    this.clock = clock;
  }

  /** Assembles the current week's insights: check-in + prioritized recommendations + timestamp. */
  public WeeklyInsights currentInsights() {
    WeeklyCheckIn checkIn = checkInService.currentCheckIn();

    List<Recommendation> all = new ArrayList<>();
    all.addAll(bodyTrendService.currentRecommendations());
    all.addAll(trainingAdherenceService.currentRecommendations());
    all.addAll(recoveryWarningService.currentRecommendations());

    List<Recommendation> ranked =
        all.stream().sorted(Comparator.comparingInt(rec -> severityRank(rec.severity()))).toList();

    Recommendation main = ranked.get(0);
    List<Recommendation> secondary = List.copyOf(ranked.subList(1, ranked.size()));

    return new WeeklyInsights(checkIn, main, secondary, Instant.now(clock));
  }

  /** Lower rank = higher priority: {@code ACTION < WARNING < INFO}. */
  private static int severityRank(RecommendationSeverity severity) {
    return switch (severity) {
      case ACTION -> 0;
      case WARNING -> 1;
      case INFO -> 2;
    };
  }
}

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
 * WeeklyCheckIn}, runs the FOR-42/43/44 rule sets plus the FOR-150 *Reglas* rule sets, selects a
 * prioritized main recommendation and returns them with a generated timestamp.
 *
 * <h2>Main-recommendation selection (documented per spec FOR-45 Open Questions)</h2>
 *
 * Recommendations are ordered by <b>severity priority</b> {@code ACTION > WARNING > INFO}; the
 * first is the {@code main} one and the rest are the secondaries. The sort is <b>stable</b>, so
 * ties keep their production order — body trend (FOR-42/FOR-150 rule 1), sustained body-fat trend
 * (FOR-150 rule 2), training adherence (FOR-43), recovery warning (FOR-44), pace degradation
 * (FOR-150 rule 4), then shopping cost (FOR-150 rule 6). Empty data still yields recommendations
 * (each of the body/training rules always emits one, e.g. an insufficient-data {@code INFO}), so
 * {@code main} is never null and the endpoint never errors on absent data.
 *
 * <h2>FOR-150 rules 3 and 5 (documented gap, not wired here)</h2>
 *
 * Rule 3 ("2 entrenos malos") needs a per-session strength-performance/"bad session" signal, and
 * rule 5 ("hambre &gt;7/10") needs a hunger check-in field; neither exists anywhere in the
 * repository today (confirmed against {@link WeeklyTrackingRecord} and the training/check-in
 * summaries — see {@link RecoveryWarningRules}'s own "per-session history" gap note, which already
 * covers rule 3's exact signal, and {@link WeeklyTrackingRecord}'s "Heart-rate field" note,
 * extended to flag the missing hunger field for rule 5). Per spec FOR-150 api.md ("rules gated on
 * missing data ... simply do not appear until their source slice lands") and AGENTS.md's ban on
 * speculative abstractions, no stub rule class or service is added for either — there is no code
 * path that could produce them, so nothing to wire. This is a documented follow-up, not an
 * oversight.
 *
 * <h2>Persistence (FOR-110)</h2>
 *
 * Every call to {@link #currentInsights()} persists the assembled result via {@link
 * InsightHistoryRepository#save(WeeklyInsights)}, keyed by the check-in's {@code weekStartDate}.
 * This wraps the existing on-demand generation path — it does not change how insights are computed,
 * it only stores the output going forward (spec FOR-110: "wrap/store its output", do not rewrite
 * generation). The returned {@link WeeklyInsights} shape is unchanged from FOR-45/FOR-56
 * (regression requirement, tests.md).
 */
@Service
public class WeeklyInsightsService {

  private final WeeklyCheckInService checkInService;
  private final BodyTrendRecommendationService bodyTrendService;
  private final BodyFatTrendRecommendationService bodyFatTrendService;
  private final TrainingAdherenceRecommendationService trainingAdherenceService;
  private final RecoveryWarningRecommendationService recoveryWarningService;
  private final PaceDegradationRecommendationService paceDegradationService;
  private final ShoppingCostRecommendationService shoppingCostService;
  private final InsightHistoryRepository historyRepository;
  private final Clock clock;

  public WeeklyInsightsService(
      WeeklyCheckInService checkInService,
      BodyTrendRecommendationService bodyTrendService,
      BodyFatTrendRecommendationService bodyFatTrendService,
      TrainingAdherenceRecommendationService trainingAdherenceService,
      RecoveryWarningRecommendationService recoveryWarningService,
      PaceDegradationRecommendationService paceDegradationService,
      ShoppingCostRecommendationService shoppingCostService,
      InsightHistoryRepository historyRepository,
      Clock clock) {
    this.checkInService = checkInService;
    this.bodyTrendService = bodyTrendService;
    this.bodyFatTrendService = bodyFatTrendService;
    this.trainingAdherenceService = trainingAdherenceService;
    this.recoveryWarningService = recoveryWarningService;
    this.paceDegradationService = paceDegradationService;
    this.shoppingCostService = shoppingCostService;
    this.historyRepository = historyRepository;
    this.clock = clock;
  }

  /**
   * Assembles the current week's insights: check-in + prioritized recommendations + timestamp, and
   * persists the result keyed by its period (FOR-110).
   */
  public WeeklyInsights currentInsights() {
    WeeklyCheckIn checkIn = checkInService.currentCheckIn();

    List<Recommendation> all = new ArrayList<>();
    all.addAll(bodyTrendService.currentRecommendations());
    all.addAll(bodyFatTrendService.currentRecommendations());
    all.addAll(trainingAdherenceService.currentRecommendations());
    all.addAll(recoveryWarningService.currentRecommendations());
    all.addAll(paceDegradationService.currentRecommendations());
    all.addAll(shoppingCostService.currentRecommendations());

    List<Recommendation> ranked =
        all.stream().sorted(Comparator.comparingInt(rec -> severityRank(rec.severity()))).toList();

    Recommendation main = ranked.get(0);
    List<Recommendation> secondary = List.copyOf(ranked.subList(1, ranked.size()));

    WeeklyInsights insights = new WeeklyInsights(checkIn, main, secondary, Instant.now(clock));
    historyRepository.save(insights);
    return insights;
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

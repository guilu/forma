package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Achievement;
import dev.diegobarrioh.forma.domain.AchievementCatalog;
import dev.diegobarrioh.forma.domain.AchievementData;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for achievements ("logros", FOR-135, achievements slice of FOR-104):
 * evaluate the closed {@link AchievementCatalog} against the owner's current data, persist any
 * newly-met rule (idempotently), and return earned (with {@code earnedAt}) + available. Evaluation
 * runs on every {@link #evaluate()} call (spec FOR-135 Open Questions: "on-GET is simplest" — MVP
 * choice over an event-driven trigger).
 *
 * <p>Reuses {@link BodyMeasurementRepository#list()} (FOR-16), {@link
 * GoalRepository#findAllByOwner} (FOR-125) and {@link IntegrationRepository#findByOwnerAndProvider}
 * (FOR-126/132) directly — each a single, cheap query — never re-deriving or duplicating their
 * counting/query logic (spec FOR-135 NFR). See {@link AchievementCatalog}'s javadoc for why {@code
 * MealLogRepository}/{@code WaterIntakeRepository} are deliberately not used here (only per-date
 * queries exist on those ports, not cheap for an all-time rule).
 *
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row, mirroring
 * {@link GoalService#OWNER_ID}/{@link MealLogService#OWNER_ID}. Duplicated here rather than a
 * shared abstraction, for the same documented reason as {@link GoalService}.
 */
@Service
public class AchievementService {

  /** Fixed single-user owner id for the MVP (ADR-002), mirroring {@link GoalService#OWNER_ID}. */
  public static final String OWNER_ID = "default-user";

  private final AchievementRepository achievementRepository;
  private final BodyMeasurementRepository bodyMeasurementRepository;
  private final GoalRepository goalRepository;
  private final IntegrationRepository integrationRepository;
  private final Clock clock;

  public AchievementService(
      AchievementRepository achievementRepository,
      BodyMeasurementRepository bodyMeasurementRepository,
      GoalRepository goalRepository,
      IntegrationRepository integrationRepository,
      Clock clock) {
    this.achievementRepository = achievementRepository;
    this.bodyMeasurementRepository = bodyMeasurementRepository;
    this.goalRepository = goalRepository;
    this.integrationRepository = integrationRepository;
    this.clock = clock;
  }

  /**
   * Evaluates every catalog rule against the owner's current data, awards (persists) any newly-met
   * rule that isn't already earned, then returns the full split of earned/available. Idempotent: a
   * rule already earned is never re-awarded or duplicated (the {@code AchievementRepository} PK,
   * migration V18, is the ultimate guarantee under concurrent evaluation; this method also skips
   * rules already in the pre-fetched earned set as a fast path). Never 404s — an owner with no data
   * yet gets an empty {@code earned} and the full {@code available} catalog (spec FOR-135 api.md).
   */
  public AchievementsView evaluate() {
    AchievementData data = loadData();

    Map<String, Instant> earnedBeforeAward = earnedById();
    Instant now = clock.instant();
    for (Achievement achievement : AchievementCatalog.all()) {
      if (!earnedBeforeAward.containsKey(achievement.id()) && achievement.rule().isMet(data)) {
        achievementRepository.awardIfNotEarned(OWNER_ID, achievement.id(), now);
      }
    }

    Map<String, Instant> earnedAfterAward = earnedById();
    List<AchievementView> earned =
        AchievementCatalog.all().stream()
            .filter(achievement -> earnedAfterAward.containsKey(achievement.id()))
            .map(achievement -> toView(achievement, earnedAfterAward.get(achievement.id())))
            .toList();
    List<AchievementView> available =
        AchievementCatalog.all().stream()
            .filter(achievement -> !earnedAfterAward.containsKey(achievement.id()))
            .map(achievement -> toView(achievement, null))
            .toList();

    return new AchievementsView(earned, available);
  }

  private Map<String, Instant> earnedById() {
    return achievementRepository.findAllByOwner(OWNER_ID).stream()
        .collect(Collectors.toMap(EarnedAchievement::achievementId, EarnedAchievement::earnedAt));
  }

  private AchievementData loadData() {
    List<BodyMeasurement> measurements = bodyMeasurementRepository.list();
    List<Goal> goals =
        goalRepository.findAllByOwner(OWNER_ID).stream().map(StoredGoal::goal).toList();
    boolean withingsSyncCompleted =
        integrationRepository
            .findByOwnerAndProvider(OWNER_ID, IntegrationProvider.WITHINGS)
            .map(AchievementService::isSuccessfulSync)
            .orElse(false);
    return new AchievementData(measurements, goals, withingsSyncCompleted);
  }

  private static boolean isSuccessfulSync(IntegrationConnection connection) {
    return connection.lastSyncOutcome() != null
        && connection.lastSyncOutcome().result() == SyncResult.OK;
  }

  private static AchievementView toView(Achievement achievement, Instant earnedAt) {
    return new AchievementView(
        achievement.id(), achievement.title(), achievement.description(), earnedAt);
  }
}

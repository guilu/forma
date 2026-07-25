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
import java.util.UUID;
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
 * <p>Real multi-user auth (FOR-145b-2, ADR-012, migration V28): every use case resolves the
 * caller's account id via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID =
 * "default-user"} constant (removed by this slice, alongside the 145b-1 interim {@code
 * requireLegacyOwner()} guard) — {@code earned_achievement}'s composite primary key was rebuilt
 * from the legacy {@code owner_id VARCHAR} column to {@code user_id UUID}.
 *
 * <p><b>{@link #bodyMeasurementRepository} scoping (145c, migration V30).</b> {@code
 * body_measurements} gained a {@code user_id} column (with a backfill), closing what was a 145c
 * "gap table" with zero owner-scoping. {@link #loadData(UUID)} now always loads the caller's own
 * measurements via {@link BodyMeasurementRepository#list(UUID)}, so measurement-based rules (e.g.
 * {@code FIRST_MEASUREMENT}, {@code TEN_MEASUREMENTS_LOGGED}) evaluate for every caller from their
 * own data. <b>145c removed the 145b-2 INTERIM security guard</b> (🟠 MEDIUM cross-account signal
 * leak) that restricted measurement-based rule evaluation to the seeded legacy placeholder account
 * and always returned an empty measurement list for every other caller.
 */
@Service
public class AchievementService {

  private final AchievementRepository achievementRepository;
  private final BodyMeasurementRepository bodyMeasurementRepository;
  private final GoalRepository goalRepository;
  private final IntegrationRepository integrationRepository;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public AchievementService(
      AchievementRepository achievementRepository,
      BodyMeasurementRepository bodyMeasurementRepository,
      GoalRepository goalRepository,
      IntegrationRepository integrationRepository,
      Clock clock,
      CurrentUserProvider currentUserProvider) {
    this.achievementRepository = achievementRepository;
    this.bodyMeasurementRepository = bodyMeasurementRepository;
    this.goalRepository = goalRepository;
    this.integrationRepository = integrationRepository;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Evaluates every catalog rule against the owner's current data, awards (persists) any newly-met
   * rule that isn't already earned, then returns the full split of earned/available. Idempotent: a
   * rule already earned is never re-awarded or duplicated (the {@code AchievementRepository} PK,
   * migration V18, is the ultimate guarantee under concurrent evaluation; this method also skips
   * rules already in the pre-fetched earned set as a fast path). Never 404s — a caller with no data
   * yet gets an empty {@code earned} and the full {@code available} catalog (spec FOR-135 api.md).
   */
  public AchievementsView evaluate() {
    UUID userId = currentUserProvider.currentUserId();
    AchievementData data = loadData(userId);

    Map<String, Instant> earnedBeforeAward = earnedById(userId);
    Instant now = clock.instant();
    for (Achievement achievement : AchievementCatalog.all()) {
      if (!earnedBeforeAward.containsKey(achievement.id()) && achievement.rule().isMet(data)) {
        achievementRepository.awardIfNotEarned(userId, achievement.id(), now);
      }
    }

    Map<String, Instant> earnedAfterAward = earnedById(userId);
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

  private Map<String, Instant> earnedById(UUID userId) {
    return achievementRepository.findAllByOwner(userId).stream()
        .collect(Collectors.toMap(EarnedAchievement::achievementId, EarnedAchievement::earnedAt));
  }

  private AchievementData loadData(UUID userId) {
    // 145c (migration V30): body_measurements now carries user_id, so this is a real per-caller
    // read -- no more INTERIM guard restricting it to the legacy placeholder account.
    List<BodyMeasurement> measurements = bodyMeasurementRepository.list(userId);
    List<Goal> goals =
        goalRepository.findAllByOwner(userId).stream().map(StoredGoal::goal).toList();
    boolean withingsSyncCompleted =
        integrationRepository
            .findByOwnerAndProvider(userId, IntegrationProvider.WITHINGS)
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

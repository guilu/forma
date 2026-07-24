package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link AchievementService} (FOR-135, achievements slice of
 * FOR-104): evaluate → award newly-met rules → return earned (with {@code earnedAt}) + available,
 * idempotent, owner-scoped (spec FOR-135 tests.md "Application Tests"). Hand-rolled in-memory fakes
 * (no Spring, no Mockito), matching {@code GoalServiceTest}/{@code AdherenceServiceTest}.
 */
class AchievementServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-17T10:00:00Z"), ZoneOffset.UTC);

  /**
   * FOR-145b-2 security fix (🟠 MEDIUM cross-account signal leak): measurement-based rules are only
   * evaluated from the still-unscoped global {@code body_measurements} table for the seeded legacy
   * placeholder account (see {@code AchievementService} javadoc's INTERIM security guard). Every
   * measurement-achievement assertion in this class therefore runs as the placeholder; {@link
   * #aNonPlaceholderUserNeverEarnsMeasurementBasedAchievementsFromTheGlobalTable()} proves the
   * guard for a real, non-placeholder caller.
   */
  private static final UUID USER_ID = LegacyUserBootstrap.PLACEHOLDER_USER_ID;

  private static final UUID OTHER_OWNER = UUID.randomUUID();

  private final FakeBodyMeasurementRepository bodyMeasurementRepository =
      new FakeBodyMeasurementRepository();
  private final FakeGoalRepository goalRepository = new FakeGoalRepository();
  private final FakeIntegrationRepository integrationRepository = new FakeIntegrationRepository();
  private final FakeAchievementRepository achievementRepository = new FakeAchievementRepository();
  private final AchievementService service =
      new AchievementService(
          achievementRepository,
          bodyMeasurementRepository,
          goalRepository,
          integrationRepository,
          FIXED_CLOCK,
          () -> USER_ID);

  @Test
  void emptyDataReturnsNoEarnedAndTheFullAvailableCatalog() {
    AchievementsView view = service.evaluate();

    assertThat(view.earned()).isEmpty();
    assertThat(view.available())
        .hasSameSizeAs(dev.diegobarrioh.forma.domain.AchievementCatalog.all());
    assertThat(view.available()).allSatisfy(a -> assertThat(a.earnedAt()).isNull());
  }

  @Test
  void evaluationAwardsANewlyMetRuleAndPersistsItWithEarnedAt() {
    bodyMeasurementRepository.saved.add(measurement());

    AchievementsView view = service.evaluate();

    assertThat(view.earned()).extracting(AchievementView::id).contains("FIRST_MEASUREMENT");
    AchievementView earned =
        view.earned().stream()
            .filter(a -> a.id().equals("FIRST_MEASUREMENT"))
            .findFirst()
            .orElseThrow();
    assertThat(earned.earnedAt()).isEqualTo(FIXED_CLOCK.instant());
    assertThat(achievementRepository.findAllByOwner(USER_ID))
        .extracting(EarnedAchievement::achievementId)
        .contains("FIRST_MEASUREMENT");
  }

  @Test
  void reEvaluatingAnAlreadyEarnedAchievementIsANoOpAndEarnedAtNeverChanges() {
    bodyMeasurementRepository.saved.add(measurement());
    AchievementsView first = service.evaluate();
    Instant firstEarnedAt =
        first.earned().stream()
            .filter(a -> a.id().equals("FIRST_MEASUREMENT"))
            .findFirst()
            .orElseThrow()
            .earnedAt();

    // Time moves on and the same rule is (still) met on the next GET.
    AchievementsView second = service.evaluate();

    assertThat(achievementRepository.findAllByOwner(USER_ID))
        .filteredOn(e -> e.achievementId().equals("FIRST_MEASUREMENT"))
        .hasSize(1);
    Instant secondEarnedAt =
        second.earned().stream()
            .filter(a -> a.id().equals("FIRST_MEASUREMENT"))
            .findFirst()
            .orElseThrow()
            .earnedAt();
    assertThat(secondEarnedAt).isEqualTo(firstEarnedAt);
  }

  @Test
  void responseSeparatesEarnedFromAvailable() {
    bodyMeasurementRepository.saved.add(measurement());

    AchievementsView view = service.evaluate();

    assertThat(view.earned())
        .extracting(AchievementView::id)
        .doesNotContainAnyElementsOf(view.available().stream().map(AchievementView::id).toList());
    assertThat(view.earned()).isNotEmpty();
    assertThat(view.available()).isNotEmpty();
  }

  @Test
  void tenMeasurementsAwardsBothMeasurementAchievements() {
    for (int i = 0; i < 10; i++) {
      bodyMeasurementRepository.saved.add(measurement());
    }

    AchievementsView view = service.evaluate();

    assertThat(view.earned())
        .extracting(AchievementView::id)
        .contains("FIRST_MEASUREMENT", "TEN_MEASUREMENTS_LOGGED");
  }

  @Test
  void firstGoalAchievedFiresOnlyWhenAGoalHasAchievedStatus() {
    goalRepository.rows.put(
        "goal-1",
        new StoredGoal(
            "goal-1",
            new Goal("Meta", GoalMetric.WEIGHT_KG, 70.0, null, GoalStatus.ACHIEVED, List.of())));

    AchievementsView view = service.evaluate();

    assertThat(view.earned())
        .extracting(AchievementView::id)
        .contains("FIRST_GOAL_CREATED", "FIRST_GOAL_ACHIEVED");
  }

  @Test
  void firstWithingsSyncFiresOnlyOnASuccessfulSyncOutcome() {
    integrationRepository.rows.put(
        IntegrationProvider.WITHINGS,
        new IntegrationConnection(
            IntegrationProvider.WITHINGS,
            IntegrationStatus.CONNECTED,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-02T00:00:00Z"),
            new SyncOutcome(SyncResult.OK, 5, 0, null)));

    AchievementsView view = service.evaluate();

    assertThat(view.earned()).extracting(AchievementView::id).contains("FIRST_WITHINGS_SYNC");
  }

  @Test
  void aFailedWithingsSyncOutcomeDoesNotEarnTheAchievement() {
    integrationRepository.rows.put(
        IntegrationProvider.WITHINGS,
        new IntegrationConnection(
            IntegrationProvider.WITHINGS,
            IntegrationStatus.NEEDS_REAUTH,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-02T00:00:00Z"),
            new SyncOutcome(SyncResult.ERROR, 0, 0, "Withings unreachable")));

    AchievementsView view = service.evaluate();

    assertThat(view.earned()).extracting(AchievementView::id).doesNotContain("FIRST_WITHINGS_SYNC");
  }

  @Test
  void onlyTheOwnersEarnedAchievementsAreReturned() {
    achievementRepository.rows.put(
        OTHER_OWNER + ":FIRST_MEASUREMENT",
        new EarnedAchievement("FIRST_MEASUREMENT", Instant.parse("2026-01-01T00:00:00Z")));

    AchievementsView view = service.evaluate();

    assertThat(view.earned()).isEmpty();
  }

  /**
   * FOR-145b-2: real per-user wiring (the 145b-1 interim {@code requireLegacyOwner()} guard was
   * removed). A different authenticated user's {@code evaluate()} call returns THEIR OWN
   * achievements (empty here, since only {@code USER_ID} has earned anything above) — never a 404,
   * and never {@code USER_ID}'s earned set.
   */
  @Test
  void aDifferentAuthenticatedUserSeesTheirOwnEmptyAchievementsNeverTheOtherUsers() {
    achievementRepository.rows.put(
        USER_ID + ":FIRST_MEASUREMENT",
        new EarnedAchievement("FIRST_MEASUREMENT", Instant.parse("2026-01-01T00:00:00Z")));
    AchievementService otherUserService =
        new AchievementService(
            achievementRepository,
            bodyMeasurementRepository,
            goalRepository,
            integrationRepository,
            FIXED_CLOCK,
            () -> OTHER_OWNER);

    AchievementsView otherUserView = otherUserService.evaluate();

    assertThat(otherUserView.earned()).isEmpty();
  }

  /**
   * FOR-145b-2 SECURITY FIX (🟠 MEDIUM cross-account signal leak, post-review): a real,
   * non-placeholder caller must never earn a measurement-based achievement from the still-unscoped
   * global {@code body_measurements} table (145c gap) — the global repository is not even consulted
   * for that caller (asserted via a call counter). Goal-based rules, which are properly
   * user-scoped, still fire normally for that same caller.
   */
  @Test
  void aNonPlaceholderUserNeverEarnsMeasurementBasedAchievementsFromTheGlobalTable() {
    // Seed the global (unscoped) table with 10+ measurements -- enough to award BOTH
    // measurement-based achievements if it were read.
    for (int i = 0; i < 10; i++) {
      bodyMeasurementRepository.saved.add(measurement());
    }
    int listCallsBefore = bodyMeasurementRepository.listCallCount;
    UUID nonPlaceholderUserId = UUID.randomUUID();
    goalRepository.rows.put(
        "goal-1",
        new StoredGoal(
            "goal-1",
            new Goal("Meta", GoalMetric.WEIGHT_KG, 70.0, null, GoalStatus.ACHIEVED, List.of())));
    AchievementService nonPlaceholderService =
        new AchievementService(
            achievementRepository,
            bodyMeasurementRepository,
            goalRepository,
            integrationRepository,
            FIXED_CLOCK,
            () -> nonPlaceholderUserId);

    AchievementsView view = nonPlaceholderService.evaluate();

    assertThat(view.earned())
        .extracting(AchievementView::id)
        .doesNotContain("FIRST_MEASUREMENT", "TEN_MEASUREMENTS_LOGGED");
    assertThat(bodyMeasurementRepository.listCallCount).isEqualTo(listCallsBefore);
    // Goal-based rules (properly user-scoped) still fire normally for this caller.
    assertThat(view.earned())
        .extracting(AchievementView::id)
        .contains("FIRST_GOAL_CREATED", "FIRST_GOAL_ACHIEVED");
  }

  /**
   * Sanity counterpart to the guard above: the seeded legacy placeholder account keeps full,
   * unchanged measurement-based achievement behavior (other tests in this class already exercise
   * this via {@code USER_ID}, which is the placeholder — this test makes that guarantee explicit).
   */
  @Test
  void thePlaceholderAccountKeepsFullMeasurementBasedAchievementBehavior() {
    assertThat(USER_ID).isEqualTo(LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    bodyMeasurementRepository.saved.add(measurement());

    AchievementsView view = service.evaluate();

    assertThat(view.earned()).extracting(AchievementView::id).contains("FIRST_MEASUREMENT");
  }

  private static BodyMeasurement measurement() {
    return new BodyMeasurement(
        Instant.parse("2026-07-01T08:00:00Z"),
        MeasurementSource.MANUAL,
        80.0,
        null,
        null,
        null,
        null,
        null);
  }

  private static class FakeBodyMeasurementRepository implements BodyMeasurementRepository {
    final List<BodyMeasurement> saved = new ArrayList<>();
    int listCallCount = 0;

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      listCallCount++;
      return List.copyOf(saved);
    }
  }

  private static class FakeGoalRepository implements GoalRepository {
    final Map<String, StoredGoal> rows = new LinkedHashMap<>();

    @Override
    public List<StoredGoal> findAllByOwner(UUID userId) {
      // This fake is single-owner for simplicity, mirroring RecordingGoalRepository.
      return List.copyOf(rows.values());
    }

    @Override
    public StoredGoal create(UUID userId, Goal goal) {
      String id = UUID.randomUUID().toString();
      StoredGoal stored = new StoredGoal(id, goal);
      rows.put(id, stored);
      return stored;
    }

    @Override
    public Optional<StoredGoal> findById(UUID userId, String goalId) {
      return Optional.ofNullable(rows.get(goalId));
    }

    @Override
    public Optional<StoredGoal> update(UUID userId, String goalId, Goal goal) {
      if (!rows.containsKey(goalId)) {
        return Optional.empty();
      }
      StoredGoal updated = new StoredGoal(goalId, goal);
      rows.put(goalId, updated);
      return Optional.of(updated);
    }
  }

  private static class FakeIntegrationRepository implements IntegrationRepository {
    final Map<IntegrationProvider, IntegrationConnection> rows = new LinkedHashMap<>();

    @Override
    public List<IntegrationConnection> findAllByOwner(UUID userId) {
      return List.copyOf(rows.values());
    }

    @Override
    public Optional<IntegrationConnection> findByOwnerAndProvider(
        UUID userId, IntegrationProvider provider) {
      return Optional.ofNullable(rows.get(provider));
    }

    @Override
    public IntegrationConnection save(UUID userId, IntegrationConnection connection) {
      rows.put(connection.provider(), connection);
      return connection;
    }
  }

  /** Keyed by {@code userId + ":" + achievementId}, mirroring the real PK shape (V18/V28). */
  private static class FakeAchievementRepository implements AchievementRepository {
    final Map<String, EarnedAchievement> rows = new LinkedHashMap<>();

    @Override
    public List<EarnedAchievement> findAllByOwner(UUID userId) {
      return rows.entrySet().stream()
          .filter(e -> e.getKey().startsWith(userId + ":"))
          .map(Map.Entry::getValue)
          .toList();
    }

    @Override
    public boolean awardIfNotEarned(UUID userId, String achievementId, Instant earnedAt) {
      String key = userId + ":" + achievementId;
      if (rows.containsKey(key)) {
        return false;
      }
      rows.put(key, new EarnedAchievement(achievementId, earnedAt));
      return true;
    }
  }
}

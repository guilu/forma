package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  private static final String OTHER_OWNER = "someone-else";

  // FOR-145b-1: matches AchievementService's internal LEGACY_OWNER_UUID compile-compat shim.
  private static final UUID LEGACY_OWNER_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

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
          () -> LEGACY_OWNER_UUID);

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
    assertThat(achievementRepository.findAllByOwner(AchievementService.OWNER_ID))
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

    assertThat(achievementRepository.findAllByOwner(AchievementService.OWNER_ID))
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

  @Test
  void aNonPlaceholderAuthenticatedCallerGets404NeverTheLegacyOwnersAchievements() {
    AchievementService otherUserService =
        new AchievementService(
            achievementRepository,
            bodyMeasurementRepository,
            goalRepository,
            integrationRepository,
            FIXED_CLOCK,
            UUID::randomUUID);

    assertThatThrownBy(otherUserService::evaluate).isInstanceOf(NotFoundException.class);
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

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
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
    public List<IntegrationConnection> findAllByOwner(String ownerId) {
      return List.copyOf(rows.values());
    }

    @Override
    public Optional<IntegrationConnection> findByOwnerAndProvider(
        String ownerId, IntegrationProvider provider) {
      return Optional.ofNullable(rows.get(provider));
    }

    @Override
    public IntegrationConnection save(String ownerId, IntegrationConnection connection) {
      rows.put(connection.provider(), connection);
      return connection;
    }
  }

  /** Keyed by {@code ownerId + ":" + achievementId}, mirroring the real PK shape (V18). */
  private static class FakeAchievementRepository implements AchievementRepository {
    final Map<String, EarnedAchievement> rows = new LinkedHashMap<>();

    @Override
    public List<EarnedAchievement> findAllByOwner(String ownerId) {
      return rows.entrySet().stream()
          .filter(e -> e.getKey().startsWith(ownerId + ":"))
          .map(Map.Entry::getValue)
          .toList();
    }

    @Override
    public boolean awardIfNotEarned(String ownerId, String achievementId, Instant earnedAt) {
      String key = ownerId + ":" + achievementId;
      if (rows.containsKey(key)) {
        return false;
      }
      rows.put(key, new EarnedAchievement(achievementId, earnedAt));
      return true;
    }
  }
}

package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link AchievementCatalog} (FOR-135, achievements slice of FOR-104): each
 * catalog rule fires exactly when its deterministic condition over fixture {@link AchievementData}
 * is met, and not before (spec FOR-135 tests.md "Rule / Domain Tests"). Pure, framework-free, no
 * Spring, no Mockito (matches {@code FoodCatalogTest}).
 */
class AchievementCatalogTest {

  private static final AchievementData EMPTY = new AchievementData(List.of(), List.of(), false);

  @Test
  void catalogIdsAreStableAndUnique() {
    List<String> ids = AchievementCatalog.all().stream().map(Achievement::id).toList();

    assertThat(ids).doesNotHaveDuplicates();
    assertThat(ids).allSatisfy(id -> assertThat(id).isNotBlank());
  }

  @Test
  void catalogContainsNoPerDateTrainingCompletionHistoryRule() {
    // Guards the documented FOR-129 gap: training_session_status stores one current status per
    // weekday slot, not a timestamped per-date history, so no achievement rule may be phrased in
    // terms of "completed on date X" training data (spec FOR-135 tests.md, ai-context.md "Common
    // Pitfalls"). The closed catalog only reuses BodyMeasurementRepository, GoalRepository and
    // IntegrationRepository — never any training/session status port.
    List<String> ids = AchievementCatalog.all().stream().map(Achievement::id).toList();

    assertThat(ids)
        .noneMatch(
            id ->
                id.toUpperCase(java.util.Locale.ROOT).contains("TRAINING")
                    || id.toUpperCase(java.util.Locale.ROOT).contains("STREAK")
                    || id.toUpperCase(java.util.Locale.ROOT).contains("SESSION"));
  }

  @Test
  void emptyDataMeetsNoRule() {
    for (Achievement achievement : AchievementCatalog.all()) {
      assertThat(achievement.rule().isMet(EMPTY))
          .as("achievement %s should not fire on empty data", achievement.id())
          .isFalse();
    }
  }

  @Test
  void firstMeasurementFiresAsSoonAsOneMeasurementExists() {
    Achievement rule = findRequired("FIRST_MEASUREMENT");
    AchievementData oneMeasurement = new AchievementData(List.of(measurement()), List.of(), false);

    assertThat(rule.rule().isMet(EMPTY)).isFalse();
    assertThat(rule.rule().isMet(oneMeasurement)).isTrue();
  }

  @Test
  void tenMeasurementsLoggedFiresAtTheThresholdNotBelow() {
    Achievement rule = findRequired("TEN_MEASUREMENTS_LOGGED");
    AchievementData nine = new AchievementData(nMeasurements(9), List.of(), false);
    AchievementData ten = new AchievementData(nMeasurements(10), List.of(), false);

    assertThat(rule.rule().isMet(nine)).isFalse();
    assertThat(rule.rule().isMet(ten)).isTrue();
  }

  @Test
  void firstGoalCreatedFiresAsSoonAsOneGoalExistsRegardlessOfStatus() {
    Achievement rule = findRequired("FIRST_GOAL_CREATED");
    AchievementData oneGoal =
        new AchievementData(List.of(), List.of(goal(GoalStatus.ACTIVE)), false);

    assertThat(rule.rule().isMet(EMPTY)).isFalse();
    assertThat(rule.rule().isMet(oneGoal)).isTrue();
  }

  @Test
  void firstGoalAchievedFiresOnlyWhenAGoalHasAchievedStatus() {
    Achievement rule = findRequired("FIRST_GOAL_ACHIEVED");
    AchievementData activeOnly =
        new AchievementData(List.of(), List.of(goal(GoalStatus.ACTIVE)), false);
    AchievementData withAchieved =
        new AchievementData(
            List.of(), List.of(goal(GoalStatus.ACTIVE), goal(GoalStatus.ACHIEVED)), false);

    assertThat(rule.rule().isMet(activeOnly)).isFalse();
    assertThat(rule.rule().isMet(withAchieved)).isTrue();
  }

  @Test
  void firstWithingsSyncFiresOnlyWhenTheFlagIsTrue() {
    Achievement rule = findRequired("FIRST_WITHINGS_SYNC");
    AchievementData notSynced = new AchievementData(List.of(), List.of(), false);
    AchievementData synced = new AchievementData(List.of(), List.of(), true);

    assertThat(rule.rule().isMet(notSynced)).isFalse();
    assertThat(rule.rule().isMet(synced)).isTrue();
  }

  private static Achievement findRequired(String id) {
    return AchievementCatalog.findById(id)
        .orElseThrow(() -> new AssertionError("Expected catalog achievement: " + id));
  }

  private static List<BodyMeasurement> nMeasurements(int n) {
    List<BodyMeasurement> result = new java.util.ArrayList<>();
    for (int i = 0; i < n; i++) {
      result.add(measurement());
    }
    return result;
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

  private static Goal goal(GoalStatus status) {
    return new Goal(
        "Meta", GoalMetric.WEIGHT_KG, 70.0, LocalDate.of(2026, 12, 31), status, List.of());
  }
}

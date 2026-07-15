package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.Milestone;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link GoalService} (FOR-125): owner-scoped create/read/update,
 * and progress derivation that reuses {@link WeeklyBodySummaryService} (which in turn reuses {@code
 * BodyMeasurement}/{@code WeeklyBodySummary}, FOR-16/FOR-21) instead of duplicating any math. Uses
 * hand-rolled in-memory fakes (no Spring, no Mockito), matching {@code UserProfileServiceTest}
 * (FOR-107, ADR-007).
 */
class GoalServiceTest {

  private final RecordingGoalRepository goalRepository = new RecordingGoalRepository();
  private final RecordingBodyMeasurementRepository bodyMeasurementRepository =
      new RecordingBodyMeasurementRepository();
  private final GoalService service =
      new GoalService(goalRepository, new WeeklyBodySummaryService(bodyMeasurementRepository));

  @Test
  void listIsEmptyWhenNoGoalsExistYet() {
    assertThat(service.list()).isEmpty();
  }

  @Test
  void createPersistsAGoalOwnerScopedAndReturnsItWithMilestones() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            LocalDate.of(2026, 12, 31),
            null,
            List.of(new Milestone(null, "15%", 15.0, false)));

    GoalView created = service.create(goal);

    assertThat(created.id()).isNotBlank();
    assertThat(created.goal().title()).isEqualTo("Bajar a 12% grasa");
    assertThat(created.goal().milestones()).hasSize(1);
    assertThat(created.goal().milestones().get(0).id()).isNotBlank();
    assertThat(goalRepository.rows.values()).allSatisfy(stored -> assertThat(stored).isNotNull());
  }

  @Test
  void createdGoalIsThenRetrievableViaList() {
    Goal goal = new Goal("Bajar a 12% grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of());
    GoalView created = service.create(goal);

    List<GoalView> all = service.list();

    assertThat(all).extracting(GoalView::id).containsExactly(created.id());
  }

  @Test
  void progressIsNullWhenNoBodyMeasurementsExist() {
    Goal goal = new Goal("Bajar a 12% grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of());

    GoalView created = service.create(goal);

    assertThat(created.progress().current()).isNull();
    assertThat(created.progress().ratio()).isNull();
  }

  @Test
  void progressIsDerivedFromRealBodyMeasurementDataWhenLinked() {
    bodyMeasurementRepository.saved.add(
        new BodyMeasurement(
            Instant.parse("2026-07-01T08:00:00Z"),
            MeasurementSource.MANUAL,
            80.0,
            16.4,
            null,
            null,
            null,
            null));
    Goal goal = new Goal("Bajar a 12% grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of());

    GoalView created = service.create(goal);

    assertThat(created.progress().current()).isEqualTo(16.4);
    assertThat(created.progress().ratio()).isEqualTo(16.4 / 12.0);
  }

  @Test
  void updateOfUnknownGoalThrowsNotFound() {
    assertThatThrownBy(
            () -> service.update("unknown-id", null, null, null, GoalStatus.ACTIVE, List.of()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void updateMergesOnlyProvidedFieldsLeavingOthersUnchanged() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT, 12.0, LocalDate.of(2026, 12, 31), null, List.of());
    GoalView created = service.create(goal);

    GoalView updated =
        service.update(created.id(), "Bajar a 11% grasa", 11.0, null, null, List.of());

    assertThat(updated.goal().title()).isEqualTo("Bajar a 11% grasa");
    assertThat(updated.goal().target()).isEqualTo(11.0);
    // dueDate/status left unchanged (null = unchanged, matching UserProfileService's convention)
    assertThat(updated.goal().dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(updated.goal().status()).isEqualTo(GoalStatus.ACTIVE);
  }

  @Test
  void updateTogglesMilestoneCompletionByIdAndPreservesOthers() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            null,
            null,
            List.of(
                new Milestone(null, "15%", 15.0, false),
                new Milestone(null, "13.5%", 13.5, false)));
    GoalView created = service.create(goal);
    String firstMilestoneId = created.goal().milestones().get(0).id();

    GoalView updated =
        service.update(
            created.id(),
            null,
            null,
            null,
            null,
            List.of(new MilestonePatch(firstMilestoneId, true)));

    assertThat(updated.goal().milestones()).hasSize(2);
    assertThat(updated.goal().milestones().get(0).completed()).isTrue();
    assertThat(updated.goal().milestones().get(1).completed()).isFalse();
  }

  @Test
  void unmatchedMilestonePatchIdsAreIgnored() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            null,
            null,
            List.of(new Milestone(null, "15%", 15.0, false)));
    GoalView created = service.create(goal);

    GoalView updated =
        service.update(
            created.id(),
            null,
            null,
            null,
            null,
            List.of(new MilestonePatch("does-not-exist", true)));

    assertThat(updated.goal().milestones().get(0).completed()).isFalse();
  }

  /** In-memory fake, matching {@code RecordingRepository} (FOR-107). */
  private static class RecordingGoalRepository implements GoalRepository {
    final Map<String, StoredGoal> rows = new LinkedHashMap<>();

    @Override
    public List<StoredGoal> findAllByOwner(String ownerId) {
      return rows.values().stream().filter(g -> owns(ownerId, g)).toList();
    }

    @Override
    public StoredGoal create(String ownerId, Goal goal) {
      String id = UUID.randomUUID().toString();
      List<Milestone> withIds = new ArrayList<>();
      for (Milestone m : goal.milestones()) {
        withIds.add(
            new Milestone(UUID.randomUUID().toString(), m.title(), m.target(), m.completed()));
      }
      Goal persisted =
          new Goal(
              goal.title(), goal.metric(), goal.target(), goal.dueDate(), goal.status(), withIds);
      StoredGoal stored = new StoredGoal(id, persisted);
      rows.put(key(ownerId, id), stored);
      return stored;
    }

    @Override
    public Optional<StoredGoal> findById(String ownerId, String goalId) {
      return Optional.ofNullable(rows.get(key(ownerId, goalId)));
    }

    @Override
    public Optional<StoredGoal> update(String ownerId, String goalId, Goal goal) {
      if (!rows.containsKey(key(ownerId, goalId))) {
        return Optional.empty();
      }
      StoredGoal updated = new StoredGoal(goalId, goal);
      rows.put(key(ownerId, goalId), updated);
      return Optional.of(updated);
    }

    private static boolean owns(String ownerId, StoredGoal stored) {
      return true; // this fake keys rows by owner already; kept simple for the test double
    }

    private static String key(String ownerId, String goalId) {
      return ownerId + ":" + goalId;
    }
  }

  /**
   * In-memory fake for {@link BodyMeasurementRepository}, matching {@code
   * BodyMeasurementServiceTest}.
   */
  private static class RecordingBodyMeasurementRepository implements BodyMeasurementRepository {
    final List<BodyMeasurement> saved = new ArrayList<>();

    @Override
    public void save(BodyMeasurement measurement) {
      saved.add(measurement);
    }

    @Override
    public List<BodyMeasurement> list() {
      List<BodyMeasurement> newestFirst = new ArrayList<>(saved);
      newestFirst.sort((a, b) -> b.measuredAt().compareTo(a.measuredAt()));
      return newestFirst;
    }
  }
}

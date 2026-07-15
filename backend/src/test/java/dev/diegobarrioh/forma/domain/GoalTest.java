package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link Goal} (FOR-125): construction validation, milestone ordering and the
 * default status/no-milestones edge cases from spec.md.
 */
class GoalTest {

  @Test
  void rejectsBlankTitle() {
    assertThatThrownBy(() -> new Goal("  ", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullMetric() {
    assertThatThrownBy(() -> new Goal("Bajar grasa", null, 12.0, null, null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonFiniteTarget() {
    assertThatThrownBy(
            () ->
                new Goal("Bajar grasa", GoalMetric.BODY_FAT_PCT, Double.NaN, null, null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void defaultsStatusToActiveWhenNotProvided() {
    Goal goal = new Goal("Bajar grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of());

    assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
  }

  @Test
  void anEmptyMilestoneListIsAllowed() {
    Goal goal = new Goal("Bajar grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, List.of());

    assertThat(goal.milestones()).isEmpty();
  }

  @Test
  void aMilestoneTargetBeyondTheGoalTargetIsAllowed() {
    Goal goal =
        new Goal(
            "Bajar grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            null,
            null,
            List.of(new Milestone(null, "20%", 20.0, false)));

    assertThat(goal.milestones().get(0).target()).isEqualTo(20.0);
  }

  @Test
  void preservesMilestoneOrder() {
    Milestone first = new Milestone(null, "15%", 15.0, false);
    Milestone second = new Milestone(null, "13.5%", 13.5, false);

    Goal goal =
        new Goal(
            "Bajar grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            LocalDate.of(2026, 12, 31),
            GoalStatus.ACTIVE,
            List.of(first, second));

    assertThat(goal.milestones()).containsExactly(first, second);
  }

  @Test
  void milestonesListIsDefensivelyCopiedAndImmutable() {
    java.util.ArrayList<Milestone> mutable = new java.util.ArrayList<>();
    mutable.add(new Milestone(null, "15%", 15.0, false));
    Goal goal = new Goal("Bajar grasa", GoalMetric.BODY_FAT_PCT, 12.0, null, null, mutable);

    mutable.clear();

    assertThat(goal.milestones()).hasSize(1);
    assertThatThrownBy(() -> goal.milestones().add(new Milestone(null, "x", 1.0, false)))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}

package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link StrengthWorkoutTemplate} and {@link StrengthWorkoutItem} (FOR-25,
 * rep-scheme extension FOR-154): construction validation for items and templates. Plain JUnit 5 +
 * AssertJ (ADR-007).
 */
class StrengthWorkoutTemplateTest {

  private static StrengthWorkoutItem item(int order) {
    return StrengthWorkoutItem.range("push-up", order, 3, 8, 12, 90, 2);
  }

  @Test
  void createsValidTemplate() {
    StrengthWorkoutTemplate template =
        new StrengthWorkoutTemplate(WorkoutType.PUSH, List.of(item(1), item(2)));

    assertThat(template.workoutType()).isEqualTo(WorkoutType.PUSH);
    assertThat(template.items()).hasSize(2);
  }

  @Nested
  class ItemValidation {

    @Test
    void rejectsBlankExerciseId() {
      assertThatThrownBy(() -> StrengthWorkoutItem.range(" ", 1, 3, 8, 12, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exerciseId");
    }

    @Test
    void rejectsRepsMaxBelowRepsMin() {
      assertThatThrownBy(() -> StrengthWorkoutItem.range("push-up", 1, 3, 12, 8, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("repsMax");
    }

    @Test
    void rejectsNonPositiveSets() {
      assertThatThrownBy(() -> StrengthWorkoutItem.range("push-up", 1, 0, 8, 12, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("sets");
    }

    @Test
    void acceptsAValidAmrapItemWithNoRepCeiling() {
      StrengthWorkoutItem amrap = StrengthWorkoutItem.amrap("push-up", 1, 3, 60, 1);

      assertThat(amrap.repScheme()).isEqualTo(RepScheme.AMRAP);
      assertThat(amrap.repsMin()).isNull();
      assertThat(amrap.repsMax()).isNull();
      assertThat(amrap.durationSecondsMin()).isNull();
      assertThat(amrap.durationSecondsMax()).isNull();
    }

    @Test
    void acceptsAValidTimeHoldItem() {
      StrengthWorkoutItem hold = StrengthWorkoutItem.timeHold("plank", 1, 3, 45, 75, 45, 2);

      assertThat(hold.repScheme()).isEqualTo(RepScheme.TIME);
      assertThat(hold.durationSecondsMin()).isEqualTo(45);
      assertThat(hold.durationSecondsMax()).isEqualTo(75);
      assertThat(hold.repsMin()).isNull();
      assertThat(hold.repsMax()).isNull();
    }

    @Test
    void rejectsRangeItemMissingRepsMax() {
      assertThatThrownBy(
              () ->
                  new StrengthWorkoutItem(
                      "push-up", 1, 3, RepScheme.RANGE, 8, null, null, null, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("repsMax");
    }

    @Test
    void rejectsAmrapItemThatAlsoSetsRepBounds() {
      assertThatThrownBy(
              () ->
                  new StrengthWorkoutItem(
                      "push-up", 1, 3, RepScheme.AMRAP, 8, 12, null, null, 60, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("repsMin");
    }

    @Test
    void rejectsAmrapItemThatAlsoSetsDurationBounds() {
      assertThatThrownBy(
              () ->
                  new StrengthWorkoutItem(
                      "push-up", 1, 3, RepScheme.AMRAP, null, null, 45, 75, 60, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("durationSecondsMin");
    }

    @Test
    void rejectsTimeHoldItemMissingDurationBounds() {
      assertThatThrownBy(
              () ->
                  new StrengthWorkoutItem(
                      "plank", 1, 3, RepScheme.TIME, null, null, 45, null, 45, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("durationSecondsMax");
    }

    @Test
    void rejectsTimeHoldItemThatAlsoSetsRepBounds() {
      assertThatThrownBy(
              () -> new StrengthWorkoutItem("plank", 1, 3, RepScheme.TIME, 8, 12, 45, 75, 45, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("repsMin");
    }

    @Test
    void rejectsDurationMaxBelowDurationMin() {
      assertThatThrownBy(() -> StrengthWorkoutItem.timeHold("plank", 1, 3, 75, 45, 45, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("durationSecondsMax");
    }

    @Test
    void rejectsNullRepScheme() {
      assertThatThrownBy(
              () -> new StrengthWorkoutItem("push-up", 1, 3, null, 8, 12, null, null, 90, 2))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("repScheme");
    }
  }

  @Nested
  class TemplateValidation {

    @Test
    void rejectsEmptyTemplate() {
      assertThatThrownBy(() -> new StrengthWorkoutTemplate(WorkoutType.PUSH, List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at least one item");
    }

    @Test
    void rejectsDuplicateItemOrder() {
      assertThatThrownBy(
              () -> new StrengthWorkoutTemplate(WorkoutType.PUSH, List.of(item(1), item(1))))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("order");
    }
  }
}

package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link StrengthWorkoutTemplate} and {@link StrengthWorkoutItem} (FOR-25):
 * construction validation for items and templates. Plain JUnit 5 + AssertJ (ADR-007).
 */
class StrengthWorkoutTemplateTest {

  private static StrengthWorkoutItem item(int order) {
    return new StrengthWorkoutItem("push-up", order, 3, 8, 12, 90, 2);
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
      assertThatThrownBy(() -> new StrengthWorkoutItem(" ", 1, 3, 8, 12, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exerciseId");
    }

    @Test
    void rejectsRepsMaxBelowRepsMin() {
      assertThatThrownBy(() -> new StrengthWorkoutItem("push-up", 1, 3, 12, 8, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("repsMax");
    }

    @Test
    void rejectsNonPositiveSets() {
      assertThatThrownBy(() -> new StrengthWorkoutItem("push-up", 1, 0, 8, 12, 90, 2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("sets");
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

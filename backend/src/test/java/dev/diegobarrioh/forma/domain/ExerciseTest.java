package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link Exercise} (FOR-24): construction validation and immutability. Plain
 * JUnit 5 + AssertJ (ADR-007).
 */
class ExerciseTest {

  private static Exercise valid(List<String> muscles) {
    return new Exercise(
        "push-up", "Flexiones", MovementPattern.PUSH, muscles, Equipment.BODYWEIGHT, "Empuja.");
  }

  @Test
  void createsValidExercise() {
    Exercise exercise = valid(List.of("pecho"));

    assertThat(exercise.id()).isEqualTo("push-up");
    assertThat(exercise.movementPattern()).isEqualTo(MovementPattern.PUSH);
    assertThat(exercise.equipment()).isEqualTo(Equipment.BODYWEIGHT);
  }

  @Test
  void rejectsBlankTextFields() {
    assertThatThrownBy(
            () ->
                new Exercise(
                    " ",
                    "Flexiones",
                    MovementPattern.PUSH,
                    List.of("pecho"),
                    Equipment.BODYWEIGHT,
                    "Empuja."))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("id");
  }

  @Test
  void rejectsEmptyPrimaryMuscles() {
    assertThatThrownBy(() -> valid(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("primaryMuscles");
  }

  @Test
  void copiesPrimaryMusclesDefensively() {
    List<String> muscles = new ArrayList<>(List.of("pecho"));
    Exercise exercise = valid(muscles);

    muscles.add("tríceps");

    // Mutating the source list must not affect the exercise.
    assertThat(exercise.primaryMuscles()).containsExactly("pecho");
  }
}

package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link LoggedSet} (training-progression-and-logging slice B, design D6):
 * construction validation for one persisted set. Plain JUnit 5 + AssertJ (ADR-007).
 */
class LoggedSetTest {

  @Test
  void createsAFullyLoggedSet() {
    LoggedSet set = new LoggedSet("push-up", 1, 62.5, 8, true);

    assertThat(set.exerciseId()).isEqualTo("push-up");
    assertThat(set.setNumber()).isEqualTo(1);
    assertThat(set.weightKg()).isEqualTo(62.5);
    assertThat(set.reps()).isEqualTo(8);
    assertThat(set.done()).isTrue();
  }

  @Test
  void acceptsAPartialSetWithOnlyWeight() {
    LoggedSet set = new LoggedSet("push-up", 1, 60.0, null, false);

    assertThat(set.weightKg()).isEqualTo(60.0);
    assertThat(set.reps()).isNull();
  }

  @Test
  void rejectsBlankExerciseId() {
    assertThatThrownBy(() -> new LoggedSet(" ", 1, 60.0, 8, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exerciseId");
  }

  @Test
  void rejectsSetNumberBelowOne() {
    assertThatThrownBy(() -> new LoggedSet("push-up", 0, 60.0, 8, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("setNumber");
  }

  @Test
  void rejectsNegativeWeight() {
    assertThatThrownBy(() -> new LoggedSet("push-up", 1, -1.0, 8, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("weightKg");
  }

  @Test
  void rejectsNegativeReps() {
    assertThatThrownBy(() -> new LoggedSet("push-up", 1, 60.0, -1, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reps");
  }
}

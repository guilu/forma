package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link Milestone} (FOR-125): construction validation and the immutable
 * completion-toggle copy method.
 */
class MilestoneTest {

  @Test
  void rejectsBlankTitle() {
    assertThatThrownBy(() -> new Milestone(null, "  ", 15.0, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonFiniteTarget() {
    assertThatThrownBy(() -> new Milestone(null, "15%", Double.NaN, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void idIsOptionalBeforePersistence() {
    Milestone milestone = new Milestone(null, "15%", 15.0, false);

    assertThat(milestone.id()).isNull();
  }

  @Test
  void withCompletedReturnsACopyWithTheNewState() {
    Milestone milestone = new Milestone("m1", "15%", 15.0, false);

    Milestone completed = milestone.withCompleted(true);

    assertThat(completed.completed()).isTrue();
    assertThat(completed.id()).isEqualTo("m1");
    assertThat(completed.title()).isEqualTo("15%");
    assertThat(completed.target()).isEqualTo(15.0);
    // original is untouched (immutability)
    assertThat(milestone.completed()).isFalse();
  }
}

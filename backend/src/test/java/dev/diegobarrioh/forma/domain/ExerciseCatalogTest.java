package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the initial {@link ExerciseCatalog} (FOR-24): coverage of push/pull/legs/
 * core, home-only equipment, unique ids, and lookup.
 */
class ExerciseCatalogTest {

  private final List<Exercise> catalog = ExerciseCatalog.exercises();

  @Test
  void coversPushPullLegsAndCore() {
    Set<MovementPattern> patterns =
        catalog.stream().map(Exercise::movementPattern).collect(Collectors.toSet());

    assertThat(patterns).contains(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.CORE);
    // "Legs" is covered by squat and/or hinge patterns.
    assertThat(patterns).containsAnyOf(MovementPattern.SQUAT, MovementPattern.HINGE);
  }

  @Test
  void everyExerciseUsesHomeEquipment() {
    Set<Equipment> home = EnumSet.allOf(Equipment.class);
    assertThat(catalog).allSatisfy(exercise -> assertThat(home).contains(exercise.equipment()));
  }

  @Test
  void idsAreUnique() {
    Set<String> ids = catalog.stream().map(Exercise::id).collect(Collectors.toSet());
    assertThat(ids).hasSameSizeAs(catalog);
  }

  @Test
  void findsExerciseByIdAndReturnsEmptyForUnknown() {
    assertThat(ExerciseCatalog.findById("push-up")).isPresent();
    assertThat(ExerciseCatalog.findById("does-not-exist")).isEmpty();
  }
}

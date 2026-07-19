package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link WorkoutTemplateCatalog} (FOR-25, rebuilt to Diego's real plan by
 * FOR-154): the three templates exist, each has 5 exercises with per-exercise sets/reps(scheme)/
 * RIR/rest matching the sheet <em>Fuerza</em> of {@code docs/fitness_os.xlsm}, and every item
 * references a real FOR-24 catalog exercise.
 */
class WorkoutTemplateCatalogTest {

  @Test
  void hasPushPullAndLegsTemplates() {
    Set<WorkoutType> types =
        WorkoutTemplateCatalog.templates().stream()
            .map(StrengthWorkoutTemplate::workoutType)
            .collect(Collectors.toSet());

    assertThat(types).contains(WorkoutType.PUSH, WorkoutType.PULL, WorkoutType.LEGS);
  }

  @Test
  void everyItemReferencesACatalogExercise() {
    assertThat(WorkoutTemplateCatalog.templates())
        .allSatisfy(
            template ->
                assertThat(template.items())
                    .allSatisfy(
                        item ->
                            assertThat(ExerciseCatalog.findById(item.exerciseId())).isPresent()));
  }

  @Test
  void everyTemplateHasExactlyFiveExercises() {
    assertThat(WorkoutTemplateCatalog.templates())
        .allSatisfy(template -> assertThat(template.items()).hasSize(5));
  }

  @Test
  void everyItemHasValidBasicInvariantsRegardlessOfScheme() {
    assertThat(WorkoutTemplateCatalog.templates())
        .allSatisfy(
            template ->
                assertThat(template.items())
                    .allSatisfy(
                        item -> {
                          assertThat(item.sets()).isGreaterThanOrEqualTo(1);
                          assertThat(item.restSeconds()).isGreaterThanOrEqualTo(0);
                          assertThat(item.rir()).isGreaterThanOrEqualTo(0);
                          if (item.repScheme() == RepScheme.RANGE) {
                            assertThat(item.repsMin()).isNotNull().isGreaterThanOrEqualTo(1);
                            assertThat(item.repsMax())
                                .isNotNull()
                                .isGreaterThanOrEqualTo(item.repsMin());
                          }
                        }));
  }

  @Test
  void resolvesTemplateByType() {
    assertThat(WorkoutTemplateCatalog.findByType(WorkoutType.PULL)).isPresent();
    assertThat(WorkoutTemplateCatalog.findByType(WorkoutType.FULL_BODY)).isEmpty();
  }

  @Test
  void pushTemplateMatchesTheFuerzaTableExactly() {
    StrengthWorkoutTemplate push =
        WorkoutTemplateCatalog.findByType(WorkoutType.PUSH).orElseThrow();

    assertThat(push.items())
        .containsExactly(
            StrengthWorkoutItem.range("dumbbell-bench-press", 1, 4, 8, 12, 90, 2),
            StrengthWorkoutItem.range("dumbbell-shoulder-press", 2, 3, 8, 10, 90, 2),
            StrengthWorkoutItem.amrap("push-up", 3, 3, 60, 1),
            StrengthWorkoutItem.range("lateral-raise", 4, 3, 12, 20, 45, 2),
            StrengthWorkoutItem.timeHold("plank", 5, 3, 45, 75, 45, 2));
  }

  @Test
  void pullTemplateMatchesTheFuerzaTableExactly() {
    StrengthWorkoutTemplate pull =
        WorkoutTemplateCatalog.findByType(WorkoutType.PULL).orElseThrow();

    assertThat(pull.items())
        .containsExactly(
            StrengthWorkoutItem.amrap("pull-up", 1, 4, 120, 1),
            StrengthWorkoutItem.range("dumbbell-row", 2, 4, 8, 12, 90, 2),
            StrengthWorkoutItem.range("band-face-pull", 3, 3, 15, 25, 45, 2),
            StrengthWorkoutItem.range("biceps-curl", 4, 3, 10, 15, 60, 2),
            StrengthWorkoutItem.range("rear-delt-fly", 5, 3, 12, 20, 45, 2));
  }

  @Test
  void legsTemplateMatchesTheFuerzaTableExactly() {
    StrengthWorkoutTemplate legs =
        WorkoutTemplateCatalog.findByType(WorkoutType.LEGS).orElseThrow();

    assertThat(legs.items())
        .containsExactly(
            StrengthWorkoutItem.range("goblet-squat", 1, 4, 10, 15, 90, 2),
            StrengthWorkoutItem.range("dumbbell-rdl", 2, 4, 8, 12, 90, 2),
            StrengthWorkoutItem.range("reverse-lunge", 3, 3, 10, 12, 90, 2),
            StrengthWorkoutItem.range("calf-raise", 4, 4, 15, 25, 45, 1),
            StrengthWorkoutItem.range("dead-bug", 5, 3, 10, 15, 45, 2));
  }

  @Test
  void amrapItemsHaveNoUpperRepBound() {
    List<StrengthWorkoutItem> amrapItems =
        WorkoutTemplateCatalog.templates().stream()
            .flatMap(template -> template.items().stream())
            .filter(item -> item.repScheme() == RepScheme.AMRAP)
            .toList();

    assertThat(amrapItems)
        .extracting(StrengthWorkoutItem::exerciseId)
        .contains("push-up", "pull-up");
    assertThat(amrapItems).allSatisfy(item -> assertThat(item.repsMax()).isNull());
  }

  @Test
  void plankIsATimedHoldNotARepRange() {
    StrengthWorkoutItem plank =
        WorkoutTemplateCatalog.findByType(WorkoutType.PUSH).orElseThrow().items().stream()
            .filter(item -> item.exerciseId().equals("plank"))
            .findFirst()
            .orElseThrow();

    assertThat(plank.repScheme()).isEqualTo(RepScheme.TIME);
    assertThat(plank.durationSecondsMin()).isEqualTo(45);
    assertThat(plank.durationSecondsMax()).isEqualTo(75);
    assertThat(plank.repsMax()).isNull();
  }
}

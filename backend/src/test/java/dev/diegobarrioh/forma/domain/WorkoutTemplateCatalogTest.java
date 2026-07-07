package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the initial {@link WorkoutTemplateCatalog} (FOR-25): the three templates
 * exist, every item references a real FOR-24 catalog exercise, and items carry sets/reps/rest.
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
  void everyItemHasSetsRepsAndRest() {
    assertThat(WorkoutTemplateCatalog.templates())
        .allSatisfy(
            template ->
                assertThat(template.items())
                    .allSatisfy(
                        item -> {
                          assertThat(item.sets()).isGreaterThanOrEqualTo(1);
                          assertThat(item.repsMin()).isGreaterThanOrEqualTo(1);
                          assertThat(item.repsMax()).isGreaterThanOrEqualTo(item.repsMin());
                          assertThat(item.restSeconds()).isGreaterThanOrEqualTo(0);
                        }));
  }

  @Test
  void resolvesTemplateByType() {
    assertThat(WorkoutTemplateCatalog.findByType(WorkoutType.PULL)).isPresent();
    assertThat(WorkoutTemplateCatalog.findByType(WorkoutType.FULL_BODY)).isEmpty();
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.CatalogExercise;
import dev.diegobarrioh.forma.application.ExerciseCatalogRepository;
import dev.diegobarrioh.forma.domain.Modality;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcExerciseCatalogRepository} (FOR-172) against the in-memory
 * PostgreSQL-mode H2 with Flyway applied (ADR-007), asserting the V24 seed data directly — no
 * {@code @BeforeEach} cleanup, since the migration IS the fixture under test.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcExerciseCatalogRepositoryTest {

  @Autowired private ExerciseCatalogRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void findAllReturnsAllTwentySeededRows() {
    List<CatalogExercise> all = repository.findAll();

    assertThat(all).hasSize(20);
    assertThat(all.stream().filter(e -> e.modality() == Modality.STRENGTH)).hasSize(16);
    assertThat(all.stream().filter(e -> e.modality() == Modality.RUNNING)).hasSize(4);
  }

  @Test
  void findByModalityFiltersStrength() {
    List<CatalogExercise> strength = repository.findByModality(Modality.STRENGTH);

    assertThat(strength).hasSize(16);
    assertThat(strength).allMatch(e -> e.modality() == Modality.STRENGTH);
  }

  @Test
  void findByModalityFiltersRunning() {
    List<CatalogExercise> running = repository.findByModality(Modality.RUNNING);

    assertThat(running).hasSize(4);
    assertThat(running).allMatch(e -> e.modality() == Modality.RUNNING);
  }

  @Test
  void findByIdReturnsStrengthExerciseWithOrderedMuscles() {
    Optional<CatalogExercise> pushUp = repository.findById("push-up");

    assertThat(pushUp).isPresent();
    CatalogExercise exercise = pushUp.get();
    assertThat(exercise.name()).isEqualTo("Flexiones");
    assertThat(exercise.modality()).isEqualTo(Modality.STRENGTH);
    assertThat(exercise.movementPattern()).isEqualTo("PUSH");
    assertThat(exercise.equipment()).isEqualTo("BODYWEIGHT");
    assertThat(exercise.instructions()).isNotBlank();
    assertThat(exercise.muscles()).containsExactly("pecho", "tríceps", "hombro anterior");
    assertThat(exercise.sessionKind()).isNull();
    assertThat(exercise.defaultDistanceKm()).isNull();
    assertThat(exercise.defaultPaceMinPerKm()).isNull();
  }

  @Test
  void findByIdReturnsRunningExerciseWithNoMusclesAndNullStrengthFields() {
    Optional<CatalogExercise> easy = repository.findById("running-easy");

    assertThat(easy).isPresent();
    CatalogExercise exercise = easy.get();
    assertThat(exercise.modality()).isEqualTo(Modality.RUNNING);
    assertThat(exercise.sessionKind()).isEqualTo("EASY");
    assertThat(exercise.muscles()).isEmpty();
    assertThat(exercise.movementPattern()).isNull();
    assertThat(exercise.equipment()).isNull();
    assertThat(exercise.instructions()).isNull();
    assertThat(exercise.defaultSets()).isNull();
    assertThat(exercise.defaultReps()).isNull();
  }

  @Test
  void findByIdOfUnknownIdReturnsEmpty() {
    assertThat(repository.findById("does-not-exist")).isEmpty();
  }

  @Test
  void everyMuscleRowReferencesAnExistingExercise() {
    Integer orphanCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM exercise_catalog_muscle m"
                + " LEFT JOIN exercise_catalog e ON e.id = m.exercise_id"
                + " WHERE e.id IS NULL",
            Integer.class);

    assertThat(orphanCount).isZero();
  }

  @Test
  void muscleTableHasThirtyOneRowsTotal() {
    Integer total =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exercise_catalog_muscle", Integer.class);

    assertThat(total).isEqualTo(31);
  }
}

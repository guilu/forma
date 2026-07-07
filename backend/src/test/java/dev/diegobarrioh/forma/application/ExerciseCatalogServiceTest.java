package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.ExerciseCatalog;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExerciseCatalogService} (FOR-24): exposes the catalog and resolves by id (no
 * Spring context — ADR-007).
 */
class ExerciseCatalogServiceTest {

  private final ExerciseCatalogService service = new ExerciseCatalogService();

  @Test
  void exposesTheCatalog() {
    assertThat(service.allExercises()).isEqualTo(ExerciseCatalog.exercises()).isNotEmpty();
  }

  @Test
  void resolvesExerciseById() {
    assertThat(service.findById("plank")).isPresent();
    assertThat(service.findById("nope")).isEmpty();
  }
}

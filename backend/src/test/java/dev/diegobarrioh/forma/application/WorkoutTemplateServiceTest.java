package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.WorkoutTemplateCatalog;
import dev.diegobarrioh.forma.domain.WorkoutType;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link WorkoutTemplateService} (FOR-25): exposes the templates and resolves by type
 * (no Spring context — ADR-007).
 */
class WorkoutTemplateServiceTest {

  private final WorkoutTemplateService service = new WorkoutTemplateService();

  @Test
  void exposesTheTemplates() {
    assertThat(service.allTemplates()).isEqualTo(WorkoutTemplateCatalog.templates()).isNotEmpty();
  }

  @Test
  void resolvesTemplateByType() {
    assertThat(service.findByType(WorkoutType.PUSH)).isPresent();
    assertThat(service.findByType(WorkoutType.FULL_BODY)).isEmpty();
  }
}

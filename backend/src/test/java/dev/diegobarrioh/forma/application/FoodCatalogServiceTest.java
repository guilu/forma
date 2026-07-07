package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.FoodCatalog;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link FoodCatalogService} (FOR-30): exposes the catalog and resolves by id (no
 * Spring context — ADR-007).
 */
class FoodCatalogServiceTest {

  private final FoodCatalogService service = new FoodCatalogService();

  @Test
  void exposesTheCatalog() {
    assertThat(service.allFoods()).isEqualTo(FoodCatalog.foods()).isNotEmpty();
  }

  @Test
  void resolvesFoodById() {
    assertThat(service.findById("oats")).isPresent();
    assertThat(service.findById("nope")).isEmpty();
  }
}

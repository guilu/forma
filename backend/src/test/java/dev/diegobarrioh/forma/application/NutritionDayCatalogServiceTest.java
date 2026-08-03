package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.NutritionDayCatalog;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.SeededFoods;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link NutritionDayCatalogService} (FOR-33): exposes the seeded days and resolves
 * by type (no Spring context — ADR-007).
 */
class NutritionDayCatalogServiceTest {

  private final NutritionDayCatalogService service =
      new NutritionDayCatalogService(SeededFoodCatalog.service());

  @Test
  void exposesTheSeededDays() {
    assertThat(service.allDays())
        .isEqualTo(NutritionDayCatalog.days(SeededFoods.LOOKUP))
        .hasSize(3);
  }

  @Test
  void resolvesDayByType() {
    assertThat(service.findByType(NutritionDayType.RUNNING)).isPresent();
  }
}

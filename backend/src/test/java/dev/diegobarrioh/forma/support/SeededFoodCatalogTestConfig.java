package dev.diegobarrioh.forma.support;

import dev.diegobarrioh.forma.application.FoodCatalogService;
import dev.diegobarrioh.forma.application.SeededFoodCatalog;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the seeded food catalog to web-slice tests.
 *
 * <p>A {@code @WebMvcTest} loads no repositories, so any test importing the real {@code
 * NutritionCalculationService} has to say where its foods come from — the calculation reads {@code
 * food_catalog} now, and mocking the lookup would leave the macro assertions asserting nothing.
 * This binds it to the same 23 seeded foods the application serves.
 */
@TestConfiguration
public class SeededFoodCatalogTestConfig {

  @Bean
  public FoodCatalogService foodCatalogService() {
    return SeededFoodCatalog.service();
  }
}

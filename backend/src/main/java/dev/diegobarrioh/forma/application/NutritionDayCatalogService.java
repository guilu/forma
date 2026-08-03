package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionDay;
import dev.diegobarrioh.forma.domain.NutritionDayCatalog;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the seeded nutrition day templates (FOR-33).
 *
 * <p>Thin service over the in-code {@link NutritionDayCatalog} so later stories (FOR-34 running-day
 * flow, and a future read endpoint) can list days or resolve one by type. Mirrors the FOR-24/FOR-30
 * catalog-service pattern.
 *
 * <p>Supplies the foods the catalog needs to compute each day's targets. The catalog holds the
 * meals — food ids and grams — but the nutrition behind those ids lives in {@code food_catalog}, so
 * targets are computed per call instead of being frozen into a static initializer.
 */
@Service
public class NutritionDayCatalogService {

  private final FoodCatalogService foods;

  public NutritionDayCatalogService(FoodCatalogService foods) {
    this.foods = foods;
  }

  /** All seeded nutrition days. */
  public List<NutritionDay> allDays() {
    return NutritionDayCatalog.days(foods);
  }

  /** Resolves a seeded nutrition day by its type. */
  public Optional<NutritionDay> findByType(NutritionDayType type) {
    return NutritionDayCatalog.findByType(type, foods);
  }
}

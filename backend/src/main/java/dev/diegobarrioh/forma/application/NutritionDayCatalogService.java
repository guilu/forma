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
 */
@Service
public class NutritionDayCatalogService {

  /** All seeded nutrition days. */
  public List<NutritionDay> allDays() {
    return NutritionDayCatalog.days();
  }

  /** Resolves a seeded nutrition day by its type. */
  public Optional<NutritionDay> findByType(NutritionDayType type) {
    return NutritionDayCatalog.findByType(type);
  }
}

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.FoodItem;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the food catalog (FOR-30).
 *
 * <p>Thin service over the in-code {@link FoodCatalog} so later stories (FOR-31 meal templates,
 * FOR-32 macro calculation, and a future read endpoint) can list foods or resolve one by id.
 * Mirrors the FOR-24 {@code ExerciseCatalogService} pattern.
 */
@Service
public class FoodCatalogService {

  /** All catalog foods. */
  public List<FoodItem> allFoods() {
    return FoodCatalog.foods();
  }

  /** Resolves a catalog food by its stable id. */
  public Optional<FoodItem> findById(String id) {
    return FoodCatalog.findById(id);
  }
}

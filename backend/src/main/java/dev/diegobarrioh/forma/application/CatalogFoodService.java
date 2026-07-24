package dev.diegobarrioh.forma.application;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the persisted food catalog (FOR-173), read-only. Thin service over
 * {@link FoodCatalogRepository}, mirroring FOR-172's {@code CatalogExerciseService} pattern. Named
 * to coexist with the static {@link FoodCatalogService}.
 */
@Service
public class CatalogFoodService {

  private final FoodCatalogRepository repository;

  public CatalogFoodService(FoodCatalogRepository repository) {
    this.repository = repository;
  }

  /** All foods in the catalog. */
  public List<CatalogFood> listAll() {
    return repository.findAll();
  }

  /** A single food by id; throws {@link NotFoundException} when no food has that id. */
  public CatalogFood getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No existe el alimento: " + id));
  }
}

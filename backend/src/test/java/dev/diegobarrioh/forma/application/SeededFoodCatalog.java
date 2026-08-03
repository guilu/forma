package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.SeededFoods;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link FoodCatalogService} over the foods V25 seeds, for application tests that need real
 * nutrition values without a database.
 *
 * <p>The service reads {@link FoodCatalogRepository}, so the fixture is a read-only in-memory
 * repository holding the same 23 rows {@code SeededFoods} describes — kept in one place rather than
 * hand-rolled in each test that happens to log a meal. Writes are unsupported on purpose: a test
 * that needs to create or edit foods is testing {@link CatalogFoodService}, which has its own
 * mutable fixture.
 */
public final class SeededFoodCatalog {

  private SeededFoodCatalog() {}

  /** A catalog service serving exactly the seeded foods. */
  public static FoodCatalogService service() {
    return new FoodCatalogService(repository());
  }

  /** The seeded foods as a read-only repository. */
  public static FoodCatalogRepository repository() {
    Map<String, CatalogFood> byId = new LinkedHashMap<>();
    SeededFoods.all().forEach(food -> byId.put(food.id(), toCatalogFood(food)));
    return new ReadOnlyRepository(byId);
  }

  private static CatalogFood toCatalogFood(FoodItem food) {
    return new CatalogFood(
        food.id(),
        food.name(),
        food.defaultServingG() == null ? null : new BigDecimal(food.defaultServingG()),
        food.kcalPer100g(),
        BigDecimal.valueOf(food.proteinPer100g()),
        BigDecimal.valueOf(food.carbsPer100g()),
        BigDecimal.valueOf(food.fatPer100g()),
        nullableDecimal(food.fiberPer100g()),
        nullableDecimal(food.sugarsPer100g()),
        nullableDecimal(food.sodiumMgPer100g()),
        nullableDecimal(food.saturatedFatPer100g()),
        // The seed predates FOR-190's group column and leaves it null; no test here depends on
        // it, and inventing one would be the fabrication FOR-134 forbids.
        null,
        // Likewise the primary macro: V44 backfills it in the database, and nothing that reads
        // this fixture asks for it.
        null);
  }

  private static BigDecimal nullableDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }

  private record ReadOnlyRepository(Map<String, CatalogFood> byId)
      implements FoodCatalogRepository {

    @Override
    public List<CatalogFood> findAll() {
      return List.copyOf(byId.values());
    }

    @Override
    public Optional<CatalogFood> findById(String id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void insert(CatalogFood food) {
      throw new UnsupportedOperationException("seeded catalog fixture is read-only");
    }

    @Override
    public void update(CatalogFood food) {
      throw new UnsupportedOperationException("seeded catalog fixture is read-only");
    }

    @Override
    public boolean delete(String id) {
      throw new UnsupportedOperationException("seeded catalog fixture is read-only");
    }
  }
}

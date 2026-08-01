package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.FoodCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Catalog maintenance use cases (FOR-190). The catalog was read-only since FOR-173; these are the
 * writes behind the admin screen. Hand-rolled in-memory repository, no Spring (ADR-007).
 */
class CatalogFoodServiceTest {

  private final InMemoryRepository repository = new InMemoryRepository();
  private final CatalogFoodService service = new CatalogFoodService(repository);

  private static CatalogFood food(String id, String name) {
    return new CatalogFood(
        id,
        name,
        new BigDecimal("100.0"),
        200,
        new BigDecimal("20.0"),
        new BigDecimal("10.0"),
        new BigDecimal("5.0"),
        null,
        null,
        null,
        null,
        FoodCategory.PROTEINA);
  }

  @Test
  void createsAFoodAndReadsItBack() {
    CatalogFood created = service.create(food("tempeh", "Tempeh"));

    assertThat(created.id()).isEqualTo("tempeh");
    assertThat(service.getById("tempeh").name()).isEqualTo("Tempeh");
  }

  /**
   * The id is the catalog's stable handle — shopping products reference it by foreign key — so a
   * second food cannot quietly take one that is in use.
   */
  @Test
  void refusesToCreateAFoodWhoseIdIsTaken() {
    service.create(food("tempeh", "Tempeh"));

    assertThatThrownBy(() -> service.create(food("tempeh", "Otro tempeh")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updatesAnExistingFood() {
    service.create(food("tempeh", "Tempeh"));

    CatalogFood updated = service.update("tempeh", food("tempeh", "Tempeh ecológico"));

    assertThat(updated.name()).isEqualTo("Tempeh ecológico");
    assertThat(service.getById("tempeh").name()).isEqualTo("Tempeh ecológico");
  }

  @Test
  void refusesToUpdateAFoodThatDoesNotExist() {
    assertThatThrownBy(() -> service.update("ghost", food("ghost", "Fantasma")))
        .isInstanceOf(NotFoundException.class);
  }

  /**
   * The id in the path wins over the id in the body: allowing a rename would silently orphan every
   * shopping product pointing at the old one.
   */
  @Test
  void ignoresAnIdInTheBodyThatContradictsThePath() {
    service.create(food("tempeh", "Tempeh"));

    service.update("tempeh", food("something-else", "Tempeh"));

    assertThat(service.getById("tempeh")).isNotNull();
    assertThatThrownBy(() -> service.getById("something-else"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deletesAFood() {
    service.create(food("tempeh", "Tempeh"));

    service.delete("tempeh");

    assertThatThrownBy(() -> service.getById("tempeh")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void refusesToDeleteAFoodThatDoesNotExist() {
    assertThatThrownBy(() -> service.delete("ghost")).isInstanceOf(NotFoundException.class);
  }

  private static final class InMemoryRepository implements FoodCatalogRepository {
    private final Map<String, CatalogFood> byId = new LinkedHashMap<>();

    @Override
    public List<CatalogFood> findAll() {
      return new ArrayList<>(byId.values());
    }

    @Override
    public Optional<CatalogFood> findById(String id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void insert(CatalogFood food) {
      byId.put(food.id(), food);
    }

    @Override
    public void update(CatalogFood food) {
      byId.put(food.id(), food);
    }

    @Override
    public boolean delete(String id) {
      return byId.remove(id) != null;
    }
  }
}

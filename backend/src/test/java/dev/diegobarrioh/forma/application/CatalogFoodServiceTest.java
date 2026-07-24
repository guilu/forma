package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogFoodService} (FOR-173). Uses a hand-rolled in-memory fake repository
 * (no Spring, no Mockito), mirroring {@code CatalogExerciseServiceTest} (ADR-007).
 */
class CatalogFoodServiceTest {

  private final RecordingRepository repository = new RecordingRepository();
  private final CatalogFoodService service = new CatalogFoodService(repository);

  private static CatalogFood oats() {
    return new CatalogFood(
        "oats",
        "Copos de avena",
        new java.math.BigDecimal("60.0"),
        370,
        new java.math.BigDecimal("13.0"),
        new java.math.BigDecimal("60.0"),
        new java.math.BigDecimal("7.0"),
        new java.math.BigDecimal("10.6"),
        new java.math.BigDecimal("0.0"),
        new java.math.BigDecimal("2.0"),
        new java.math.BigDecimal("1.2"));
  }

  @Test
  void listAllDelegatesToFindAll() {
    repository.all.add(oats());

    assertThat(service.listAll()).containsExactly(oats());
  }

  @Test
  void getByIdReturnsStoredFood() {
    repository.byId.put("oats", oats());

    assertThat(service.getById("oats")).isEqualTo(oats());
  }

  @Test
  void getByIdThrowsNotFoundWhenMissing() {
    assertThatThrownBy(() -> service.getById("nope")).isInstanceOf(NotFoundException.class);
  }

  /** In-memory {@link FoodCatalogRepository} fake. */
  private static final class RecordingRepository implements FoodCatalogRepository {
    private final List<CatalogFood> all = new ArrayList<>();
    private final Map<String, CatalogFood> byId = new HashMap<>();

    @Override
    public List<CatalogFood> findAll() {
      return List.copyOf(all);
    }

    @Override
    public Optional<CatalogFood> findById(String id) {
      return Optional.ofNullable(byId.get(id));
    }
  }
}

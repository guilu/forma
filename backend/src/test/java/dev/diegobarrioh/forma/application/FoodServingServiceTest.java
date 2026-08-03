package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Curating a food's portions (V49). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>The interesting rule is that a food has at most one default, which the database enforces with
 * a nullable sentinel. Swapping it means clearing the old one before setting the new, and the order
 * is not an implementation detail: the other way round trips the unique index.
 */
class FoodServingServiceTest {

  private final InMemoryServings repository = new InMemoryServings();
  private final FoodCatalogService foods = SeededFoodCatalog.service();
  private final FoodServingService service = new FoodServingService(repository, foods);

  private static FoodServing named(String foodId, String name, String grams, boolean isDefault) {
    return new FoodServing(null, foodId, name, new BigDecimal(grams), isDefault, 0);
  }

  @Test
  void addsANamedPortionAlongsideTheOneTheFoodAlreadyHad() {
    repository.save(FoodServing.plainDefault("banana", new BigDecimal("120.0")));

    service.create(named("banana", "Grande", "150.0", false));

    assertThat(service.findByFood("banana"))
        .extracting(FoodServing::name)
        .containsExactlyInAnyOrder(null, "Grande");
  }

  /** Two portions of one food called the same thing is somebody having written it twice. */
  @Test
  void refusesTwoPortionsOfOneFoodUnderTheSameName() {
    service.create(named("banana", "Grande", "150.0", false));

    assertThatThrownBy(() -> service.create(named("banana", "Grande", "160.0", false)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Grande");
  }

  /** The same name under two different foods is two different portions. */
  @Test
  void letsTwoFoodsUseTheSamePortionName() {
    service.create(named("banana", "Grande", "150.0", false));

    assertThat(service.create(named("potato", "Grande", "300.0", false))).isNotNull();
  }

  /**
   * Promoting a portion demotes the one that held the place. Done the other way round it would trip
   * the unique index that keeps "one serving" meaning one thing.
   */
  @Test
  void promotingAPortionDemotesThePreviousDefault() {
    repository.save(FoodServing.plainDefault("banana", new BigDecimal("120.0")));
    FoodServing big = service.create(named("banana", "Grande", "150.0", false));

    service.update(big.id(), named("banana", "Grande", "150.0", true));

    assertThat(service.findByFood("banana"))
        .filteredOn(FoodServing::isDefault)
        .singleElement()
        .satisfies(serving -> assertThat(serving.name()).isEqualTo("Grande"));
  }

  /**
   * Promoting the one that is already default is not a change, and must not demote it to nothing.
   */
  @Test
  void promotingTheDefaultAgainLeavesItTheDefault() {
    FoodServing only = service.create(named("banana", "Mediano", "120.0", true));

    service.update(only.id(), named("banana", "Mediano", "125.0", true));

    assertThat(service.findByFood("banana"))
        .singleElement()
        .satisfies(
            serving -> {
              assertThat(serving.isDefault()).isTrue();
              assertThat(serving.grams()).isEqualByComparingTo("125.0");
            });
  }

  /** A food may end up with no default: that is the state every unportioned food is already in. */
  @Test
  void letsTheLastDefaultBeRemoved() {
    FoodServing only = service.create(named("banana", "Mediano", "120.0", true));

    service.delete(only.id());

    assertThat(service.findByFood("banana")).isEmpty();
  }

  @Test
  void refusesAPortionOfAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> service.create(named("unicornio", "Ala", "100.0", false)))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unicornio");
  }

  @Test
  void refusesToChangeAPortionNobodyWrote() {
    assertThatThrownBy(() -> service.update("nope", named("banana", "Grande", "150.0", false)))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.delete("nope")).isInstanceOf(NotFoundException.class);
  }

  /** Moving a portion to a different food would silently reassign it; the path owns the food. */
  @Test
  void refusesToMoveAPortionToADifferentFood() {
    FoodServing big = service.create(named("banana", "Grande", "150.0", false));

    assertThatThrownBy(() -> service.update(big.id(), named("potato", "Grande", "300.0", false)))
        .isInstanceOf(ValidationException.class);
  }

  private static final class InMemoryServings implements FoodServingRepository {
    private final Map<String, FoodServing> rows = new LinkedHashMap<>();

    @Override
    public List<FoodServing> findByFood(String foodId) {
      List<FoodServing> found = new ArrayList<>();
      for (FoodServing row : rows.values()) {
        if (row.foodId().equals(foodId)) {
          found.add(row);
        }
      }
      return found;
    }

    @Override
    public Optional<FoodServing> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public Optional<FoodServing> findDefault(String foodId) {
      return findByFood(foodId).stream().filter(FoodServing::isDefault).findFirst();
    }

    @Override
    public void save(FoodServing serving) {
      rows.put(serving.id(), serving);
    }

    @Override
    public void clearDefault(String foodId) {
      findDefault(foodId)
          .ifPresent(
              current ->
                  rows.put(
                      current.id(),
                      new FoodServing(
                          current.id(),
                          current.foodId(),
                          current.name(),
                          current.grams(),
                          false,
                          current.sortOrder())));
    }

    @Override
    public boolean delete(String id) {
      return rows.remove(id) != null;
    }

    @Override
    public void deleteByFood(String foodId) {
      rows.values().removeIf(row -> row.foodId().equals(foodId));
    }

    @Override
    public boolean deleteDefault(String foodId) {
      return rows.values().removeIf(row -> row.foodId().equals(foodId) && row.isDefault());
    }
  }
}

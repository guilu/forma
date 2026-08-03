package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import dev.diegobarrioh.forma.domain.EquivalenceBasis;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Curating which food stands in for which (V47). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>The rule that computes an equivalence is the same one that validates it: a swap whose grams
 * cannot be worked out is a swap that must not be stored, so the service asks for the answer and
 * lets the refusal travel. That is why there is no second copy of the zero check here.
 */
class FoodEquivalenceServiceTest {

  private final InMemoryEquivalences repository = new InMemoryEquivalences();
  private final MutableFoods catalog = new MutableFoods();
  private final FoodCatalogService foods = new FoodCatalogService(catalog);
  private final FoodEquivalenceService service = new FoodEquivalenceService(repository, foods);

  private static FoodEquivalence equivalence(String source, String target, EquivalenceBasis basis) {
    return new FoodEquivalence(
        null, source, target, basis, new BigDecimal("100.0"), null, null, true, null, null);
  }

  @Test
  void storesASubstitutionAndReadsItBackWithItsGramsWorkedOut() {
    service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));

    assertThat(service.findBySource("rice"))
        .singleElement()
        .satisfies(
            resolved -> {
              assertThat(resolved.targetName()).isEqualTo("Patata");
              // 100 g rice = 79 g carbs; potato carries 17 g per 100 g.
              assertThat(resolved.portion().targetReferenceG()).isCloseTo(464.7, within(0.1));
            });
  }

  /** The grams are never stored, so correcting a food's macros moves every answer that used it. */
  @Test
  void followsTheCatalogWhenAFoodsMacrosChange() {
    service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));
    double before = service.findBySource("rice").get(0).portion().targetReferenceG();

    catalog.doubleCarbsOf("potato");
    double after = service.findBySource("rice").get(0).portion().targetReferenceG();

    assertThat(after).isCloseTo(before / 2, within(0.1));
  }

  /** A swap whose grams cannot be worked out is a swap that must not be stored. */
  @Test
  void refusesASubstitutionThatCannotBeWorkedOut() {
    assertThatThrownBy(() -> service.create(equivalence("rice", "chicken", EquivalenceBasis.CARBS)))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("CARBS");
    assertThatThrownBy(
            () -> service.create(equivalence("olive-oil", "rice", EquivalenceBasis.PROTEIN)))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("PROTEIN");
  }

  @Test
  void refusesAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(
            () -> service.create(equivalence("rice", "unicornio", EquivalenceBasis.CARBS)))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unicornio");
  }

  /** Rice for potato on carbohydrate and on calories are two different pieces of advice. */
  @Test
  void allowsTheSamePairOnDifferentGrounds() {
    service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));

    assertThat(service.create(equivalence("rice", "potato", EquivalenceBasis.CALORIES)))
        .isNotNull();
    assertThat(service.findBySource("rice")).hasSize(2);
  }

  @Test
  void refusesTheSamePairTwiceOnTheSameGrounds() {
    service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));

    assertThatThrownBy(() -> service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS)))
        .isInstanceOf(ConflictException.class);
  }

  /** Advice in one direction is not advice in the other, so the inverse is not conjured. */
  @Test
  void doesNotInventTheOppositeDirection() {
    service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));

    assertThat(service.findBySource("potato")).isEmpty();
  }

  /** A retired substitution stops being offered without the row, or its history, disappearing. */
  @Test
  void leavesOutSubstitutionsSomebodyRetired() {
    FoodEquivalence stored = service.create(equivalence("rice", "potato", EquivalenceBasis.CARBS));
    repository.retire(stored.id());

    assertThat(service.findBySource("rice")).isEmpty();
  }

  /** The tolerance is reported, never enforced: the swap delivers the nutrient it promised. */
  @Test
  void reportsWhetherTheDriftExceedsTheStatedToleranceWithoutRefusing() {
    FoodEquivalence tight =
        new FoodEquivalence(
            null,
            "rice",
            "potato",
            EquivalenceBasis.CARBS,
            new BigDecimal("100.0"),
            new BigDecimal("25.0"),
            null,
            true,
            null,
            null);

    service.create(tight);

    assertThat(service.findBySource("rice"))
        .singleElement()
        .satisfies(resolved -> assertThat(resolved.exceedsTolerance()).isTrue());
  }

  /**
   * The seeded foods, but writable. The point of one test here is that correcting a food moves
   * every equivalence that used it, which needs a catalog that can actually be corrected.
   */
  private static final class MutableFoods implements FoodCatalogRepository {
    private final Map<String, CatalogFood> rows = new LinkedHashMap<>();

    MutableFoods() {
      SeededFoodCatalog.repository().findAll().forEach(food -> rows.put(food.id(), food));
    }

    void doubleCarbsOf(String id) {
      CatalogFood food = rows.get(id);
      rows.put(
          id,
          new CatalogFood(
              food.id(),
              food.name(),
              food.servingSizeG(),
              food.kcal(),
              food.proteinG(),
              food.carbsG().multiply(new BigDecimal("2")),
              food.fatG(),
              food.fiberG(),
              food.sugarsG(),
              food.sodiumMg(),
              food.saturatedFatG(),
              food.foodGroupId(),
              food.primaryMacro()));
    }

    @Override
    public List<CatalogFood> findAll() {
      return List.copyOf(rows.values());
    }

    @Override
    public Optional<CatalogFood> findById(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void insert(CatalogFood food) {
      rows.put(food.id(), food);
    }

    @Override
    public void update(CatalogFood food) {
      rows.put(food.id(), food);
    }

    @Override
    public boolean delete(String id) {
      return rows.remove(id) != null;
    }
  }

  private static final class InMemoryEquivalences implements FoodEquivalenceRepository {
    private final Map<UUID, FoodEquivalence> rows = new LinkedHashMap<>();

    void retire(UUID id) {
      FoodEquivalence row = rows.get(id);
      rows.put(
          id,
          new FoodEquivalence(
              row.id(),
              row.sourceFoodId(),
              row.targetFoodId(),
              row.basis(),
              row.sourceReferenceG(),
              row.maxMacroDeviationPct(),
              row.notes(),
              false,
              row.createdAt(),
              row.updatedAt()));
    }

    @Override
    public List<FoodEquivalence> findBySource(String sourceFoodId) {
      List<FoodEquivalence> found = new ArrayList<>();
      for (FoodEquivalence row : rows.values()) {
        if (row.sourceFoodId().equals(sourceFoodId) && row.enabled()) {
          found.add(row);
        }
      }
      return found;
    }

    @Override
    public Optional<FoodEquivalence> find(
        String sourceFoodId, String targetFoodId, EquivalenceBasis basis) {
      return rows.values().stream()
          .filter(
              row ->
                  row.sourceFoodId().equals(sourceFoodId)
                      && row.targetFoodId().equals(targetFoodId)
                      && row.basis() == basis)
          .findFirst();
    }

    @Override
    public void insert(FoodEquivalence equivalence) {
      rows.put(equivalence.id(), equivalence);
    }

    @Override
    public boolean delete(UUID id) {
      return rows.remove(id) != null;
    }
  }
}

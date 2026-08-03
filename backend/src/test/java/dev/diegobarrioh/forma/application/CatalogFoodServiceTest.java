package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.PrimaryMacro;
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
  private final InMemoryGroups groups = new InMemoryGroups();
  private final InMemoryServings servings = new InMemoryServings();
  private final CatalogFoodService service = new CatalogFoodService(repository, groups, servings);

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
        "PROTEINA",
        null,
        null);
  }

  /**
   * A food arriving without a primary macro gets the one its own numbers imply — 20 g protein (80
   * kcal) against 10 g carbs (40 kcal) and 5 g fat (45 kcal). Leaving it null would put a column
   * nothing fills next to the data that fills it.
   */
  @Test
  void classifiesAFoodByItsMacrosWhenNobodySaysOtherwise() {
    CatalogFood created = service.create(food("tempeh", "Tempeh"));

    assertThat(created.primaryMacro()).isEqualTo(PrimaryMacro.PROTEIN);
    assertThat(service.getById("tempeh").primaryMacro()).isEqualTo(PrimaryMacro.PROTEIN);
  }

  /**
   * The computed answer is a default, not a verdict. A protein yogurt is sold and eaten as a
   * protein even when its label's carbohydrates edge it out, and whoever curates the catalog gets
   * to say so.
   */
  @Test
  void keepsThePrimaryMacroSomebodyChose() {
    CatalogFood chosen = withPrimaryMacro(food("yogurt", "Yogur proteína"), PrimaryMacro.PROTEIN);

    assertThat(service.create(chosen).primaryMacro()).isEqualTo(PrimaryMacro.PROTEIN);
  }

  /** A food whose macros decide nothing keeps an empty answer rather than an invented one. */
  @Test
  void leavesTheMacroUnsetWhenNothingDominates() {
    CatalogFood water =
        new CatalogFood(
            "water",
            "Agua",
            new BigDecimal("250.0"),
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertThat(service.create(water).primaryMacro()).isNull();
  }

  /**
   * Editing the macros without saying anything about the macro re-derives it: a food whose fat was
   * corrected upwards should stop claiming to be a protein just because it was one before.
   */
  @Test
  void reclassifiesOnUpdateWhenTheEditSaysNothingAboutIt() {
    service.create(food("tempeh", "Tempeh"));

    CatalogFood fattier =
        new CatalogFood(
            "tempeh",
            "Tempeh",
            new BigDecimal("100.0"),
            200,
            new BigDecimal("20.0"),
            new BigDecimal("10.0"),
            new BigDecimal("30.0"), // 270 kcal of fat against 80 of protein
            null,
            null,
            null,
            null,
            "PROTEINA",
            null,
            null);

    assertThat(service.update("tempeh", fattier).primaryMacro()).isEqualTo(PrimaryMacro.FAT);
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
  private static CatalogFood withPrimaryMacro(CatalogFood food, PrimaryMacro macro) {
    return new CatalogFood(
        food.id(),
        food.name(),
        food.servingSizeG(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        food.foodGroupId(),
        macro,
        null);
  }

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

  /**
   * The groups are rows with a foreign key behind them since V43, so a food filed under one that
   * does not exist has to be refused here. Left to the database it trips the constraint and
   * surfaces as a server error, which blames the wrong thing.
   */
  @Test
  void refusesAFoodFiledUnderAGroupThatIsNotOneOfOurs() {
    CatalogFood bogus = withFoodGroup(food("tempeh", "Tempeh"), "INVENTADO");

    assertThatThrownBy(() -> service.create(bogus))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("INVENTADO");

    service.create(food("tempeh", "Tempeh"));
    assertThatThrownBy(() -> service.update("tempeh", bogus))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("INVENTADO");
  }

  /** A food nobody has classified is not a food filed under a missing group. */
  @Test
  void acceptsAFoodWithNoGroupAtAll() {
    CatalogFood unclassified = withFoodGroup(food("tempeh", "Tempeh"), null);

    assertThat(service.create(unclassified).foodGroupId()).isNull();
  }

  private static CatalogFood withFoodGroup(CatalogFood food, String groupId) {
    return new CatalogFood(
        food.id(),
        food.name(),
        food.servingSizeG(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        groupId,
        food.primaryMacro(),
        null);
  }

  private static final class InMemoryGroups implements FoodGroupRepository {
    private final Map<String, FoodGroup> rows =
        new LinkedHashMap<>(
            Map.of("PROTEINA", new FoodGroup("PROTEINA", "Proteína", "🍗", null, 2, true)));

    @Override
    public List<FoodGroup> findAll() {
      return List.copyOf(rows.values());
    }

    @Override
    public Optional<FoodGroup> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void update(FoodGroup group) {
      rows.put(group.id(), group);
    }
  }

  // --- V49: the portion moved out of food_catalog and into its own table ---

  /** A food created with a portion gets a row for it, as its default and under no name. */
  @Test
  void writesThePortionAsARowOfItsOwn() {
    service.create(food("tempeh", "Tempeh"));

    assertThat(servings.findDefault("tempeh"))
        .get()
        .satisfies(
            serving -> {
              assertThat(serving.grams()).isEqualByComparingTo("100.0");
              assertThat(serving.name()).isNull();
              assertThat(serving.isDefault()).isTrue();
            });
  }

  /** And reads back the same way it always did, so nothing above this notices the move. */
  @Test
  void stillReportsThePortionOnTheFood() {
    service.create(food("tempeh", "Tempeh"));

    assertThat(service.getById("tempeh").servingSizeG()).isEqualByComparingTo("100.0");
  }

  @Test
  void changesThePortionWhenTheFoodIsEdited() {
    service.create(food("tempeh", "Tempeh"));

    service.update("tempeh", withServing(food("tempeh", "Tempeh"), new BigDecimal("150.0")));

    assertThat(servings.findDefault("tempeh"))
        .get()
        .satisfies(serving -> assertThat(serving.grams()).isEqualByComparingTo("150.0"));
  }

  /**
   * Clearing the portion removes the default and nothing else. A food's named portions are somebody
   * else's work and an edit to this form never asked about them.
   */
  @Test
  void clearingThePortionLeavesTheNamedOnesAlone() {
    service.create(food("banana", "Plátano"));
    servings.save(new FoodServing("s1", "banana", "Grande", new BigDecimal("150.0"), false, 1));

    service.update("banana", withServing(food("banana", "Plátano"), null));

    assertThat(servings.findDefault("banana")).isEmpty();
    assertThat(servings.findByFood("banana"))
        .singleElement()
        .satisfies(serving -> assertThat(serving.name()).isEqualTo("Grande"));
  }

  /**
   * A food nobody has given a portion to is a real state, and it gets no row rather than a zero.
   */
  @Test
  void writesNoPortionForAFoodThatStatesNone() {
    service.create(withServing(food("tempeh", "Tempeh"), null));

    assertThat(servings.findByFood("tempeh")).isEmpty();
    assertThat(service.getById("tempeh").servingSizeG()).isNull();
  }

  /**
   * Deleting a food takes its portions with it. They are part of the food, not references to it,
   * and leaving them would have the foreign key refuse an ordinary delete as a server error.
   */
  @Test
  void deletingAFoodTakesItsPortionsWithIt() {
    service.create(food("banana", "Plátano"));
    servings.save(new FoodServing("s1", "banana", "Grande", new BigDecimal("150.0"), false, 1));

    service.delete("banana");

    assertThat(servings.findByFood("banana")).isEmpty();
  }

  private static CatalogFood withServing(CatalogFood food, BigDecimal grams) {
    return new CatalogFood(
        food.id(),
        food.name(),
        grams,
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        food.foodGroupId(),
        food.primaryMacro(),
        null);
  }

  private static final class InMemoryServings implements FoodServingRepository {
    private final Map<String, FoodServing> rows = new LinkedHashMap<>();

    @Override
    public List<FoodServing> findByFood(String foodId) {
      return rows.values().stream().filter(row -> row.foodId().equals(foodId)).toList();
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
    public Optional<FoodServing> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void clearDefault(String foodId) {
      throw new UnsupportedOperationException();
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

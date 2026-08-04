package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Curating recipes (V52). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>A recipe holds no nutrition of its own, so the interesting behaviour is that its totals follow
 * the catalog — and that per-serving is the whole divided by how many portions it makes, which is
 * the figure anybody eating it actually wants.
 */
class RecipeServiceTest {

  private final InMemoryRecipes repository = new InMemoryRecipes();
  private final MutableFoods catalog = new MutableFoods();
  private final FoodCatalogService foods = new FoodCatalogService(catalog);
  private final RecipeService service = new RecipeService(repository, foods);

  private static Recipe recipe(String id, String name, int servings, RecipeIngredient... items) {
    return new Recipe(id, name, servings, null, true, List.of(items), null, null);
  }

  private static RecipeIngredient of(String foodId, String grams) {
    return new RecipeIngredient(foodId, new BigDecimal(grams), 0);
  }

  @Test
  void addsARecipeAndSumsItsIngredientsFromTheCatalog() {
    // 60 g oats (370 kcal/100g) + 200 g skimmed milk (35 kcal/100g) = 222 + 70 = 292.
    service.create(recipe("avena", "Avena overnight", 1, of("oats", "60"), of("skim-milk", "200")));

    assertThat(service.findById("avena").total().calories()).isEqualTo(292);
  }

  /** The figure anybody eating it wants, and the one that is wrong fourfold for a stew for four. */
  @Test
  void dividesTheWholeByHowManyPortionsItMakes() {
    service.create(recipe("guiso", "Guiso", 4, of("rice", "400")));

    ResolvedRecipe resolved = service.findById("guiso");

    assertThat(resolved.total().calories()).isEqualTo(1440); // 400 g rice at 360/100g
    assertThat(resolved.perServing().calories()).isEqualTo(360);
    assertThat(resolved.perServing().carbsG()).isCloseTo(79.0, within(0.05));
  }

  /** Nothing is stored, so correcting a food moves every recipe that uses it. */
  @Test
  void followsTheCatalogWhenAFoodsMacrosChange() {
    service.create(recipe("avena", "Avena overnight", 1, of("oats", "60")));
    int before = service.findById("avena").total().calories();

    catalog.doubleCaloriesOf("oats");

    assertThat(service.findById("avena").total().calories()).isEqualTo(before * 2);
  }

  @Test
  void refusesAnIngredientThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> service.create(recipe("x", "X", 1, of("unicornio", "60"))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unicornio");
  }

  @Test
  void refusesTwoRecipesWithTheSameId() {
    service.create(recipe("avena", "Avena overnight", 1, of("oats", "60")));

    assertThatThrownBy(() -> service.create(recipe("avena", "Otra avena", 1, of("oats", "60"))))
        .isInstanceOf(ConflictException.class);
  }

  /** A dish with nothing in it is not a dish; it would total zero and mean nothing. */
  @Test
  void refusesADishWithNoIngredients() {
    assertThatThrownBy(() -> service.create(recipe("vacia", "Vacía", 1)))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void replacesTheIngredientsWhenTheDishIsEdited() {
    service.create(recipe("avena", "Avena overnight", 1, of("oats", "60"), of("banana", "120")));

    service.update("avena", recipe("avena", "Avena overnight", 1, of("oats", "60")));

    assertThat(service.findById("avena").recipe().ingredients()).hasSize(1);
  }

  @Test
  void refusesToReadOrRemoveADishNobodyWrote() {
    assertThatThrownBy(() -> service.findById("nope")).isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.delete("nope")).isInstanceOf(NotFoundException.class);
  }

  /**
   * The seeded foods, writable. One test corrects a food and watches every recipe using it move,
   * which is the whole reason a recipe stores no totals.
   */
  private static final class MutableFoods implements FoodCatalogRepository {
    private final Map<String, CatalogFood> rows = new LinkedHashMap<>();

    MutableFoods() {
      SeededFoodCatalog.repository().findAll().forEach(food -> rows.put(food.id(), food));
    }

    void doubleCaloriesOf(String id) {
      CatalogFood food = rows.get(id);
      rows.put(
          id,
          new CatalogFood(
              food.id(),
              food.name(),
              food.servingSizeG(),
              food.kcal() * 2,
              food.proteinG(),
              food.carbsG(),
              food.fatG(),
              food.fiberG(),
              food.sugarsG(),
              food.sodiumMg(),
              food.saturatedFatG(),
              food.foodGroupId(),
              food.primaryMacro(),
              food.preparation()));
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

  private static final class InMemoryRecipes implements RecipeRepository {
    private final Map<String, Recipe> rows = new LinkedHashMap<>();

    @Override
    public List<Recipe> findAll() {
      return new ArrayList<>(rows.values());
    }

    @Override
    public Optional<Recipe> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void save(Recipe recipe) {
      rows.put(recipe.id(), recipe);
    }

    @Override
    public boolean delete(String id) {
      return rows.remove(id) != null;
    }
  }
}

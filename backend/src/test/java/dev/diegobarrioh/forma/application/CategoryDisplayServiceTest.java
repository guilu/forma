package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.CategoryScope;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Renaming and re-drawing a category (FOR-197). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>The two vocabularies are stored differently since V43: a food group is a row in {@code
 * food_group}, an aisle is a row in {@code category_display}. This endpoint hides that — every
 * screen that shows a category wants the same three things from it — so these tests pin that both
 * are served and edited alike, and that neither leaks into the other.
 *
 * <p>There is still no create and no delete: a food group deletion would orphan the foods pointing
 * at it (the foreign key refuses), and the aisle set is closed in the domain enum. What an admin
 * owns is how each one is written and drawn.
 */
class CategoryDisplayServiceTest {

  private final InMemoryDisplays displays = new InMemoryDisplays();
  private final InMemoryGroups groups = new InMemoryGroups();
  private final CategoryDisplayService service = new CategoryDisplayService(displays, groups);

  @Test
  void listsBothVocabularies() {
    groups.seed(new FoodGroup("PROTEINA", "Proteína", "🍗", null, 2, true));
    displays.seed(new CategoryDisplay(CategoryScope.SHOPPING, "OTROS", "Otros", "🛒"));

    assertThat(service.findAll(null)).hasSize(2);
    assertThat(service.findAll(CategoryScope.FOOD))
        .extracting(CategoryDisplay::code)
        .containsExactly("PROTEINA");
  }

  /** A group carries its own place in the list; the screen should not have to sort it again. */
  @Test
  void listsFoodGroupsInTheirOwnOrderNotAlphabetically() {
    groups.seed(new FoodGroup("SUPLEMENTO", "Suplemento", "💊", null, 10, true));
    groups.seed(new FoodGroup("CARBOHIDRATO", "Carbohidrato", "🌾", null, 1, true));

    assertThat(service.findAll(CategoryScope.FOOD))
        .extracting(CategoryDisplay::code)
        .containsExactly("CARBOHIDRATO", "SUPLEMENTO");
  }

  @Test
  void renamesAFoodGroupAndChangesItsIcon() {
    groups.seed(new FoodGroup("LACTEO", "Lácteo", "🥛", null, 6, true));

    CategoryDisplay updated =
        service.update(CategoryScope.FOOD, "LACTEO", "Lácteos y derivados", "🧀");

    assertThat(updated.label()).isEqualTo("Lácteos y derivados");
    assertThat(updated.icon()).isEqualTo("🧀");
  }

  /**
   * A rename touches how the group reads and nothing else. Its place in the list and whether it is
   * offered at all are separate decisions, and an edit that quietly reset them would be a surprise.
   */
  @Test
  void renamingAGroupLeavesItsOrderAndAvailabilityAlone() {
    groups.seed(new FoodGroup("BEBIDA", "Bebida", "🥤", "#3366ff", 8, false));

    service.update(CategoryScope.FOOD, "BEBIDA", "Bebidas", "🧃");

    assertThat(groups.find("BEBIDA"))
        .get()
        .satisfies(
            group -> {
              assertThat(group.sortOrder()).isEqualTo(8);
              assertThat(group.enabled()).isFalse();
              assertThat(group.color()).isEqualTo("#3366ff");
            });
  }

  @Test
  void renamesAnAisleAndChangesItsIcon() {
    displays.seed(new CategoryDisplay(CategoryScope.SHOPPING, "OTROS", "Otros", "🛒"));

    CategoryDisplay updated = service.update(CategoryScope.SHOPPING, "OTROS", "Varios", "📦");

    assertThat(updated.label()).isEqualTo("Varios");
    assertThat(updated.icon()).isEqualTo("📦");
  }

  /**
   * The code is the value every row points at. An update that could introduce one would be creating
   * a category by the back door — and one nothing may ever be filed under, since the foreign key
   * (food groups) or the CHECK (aisles) would refuse it.
   */
  @Test
  void refusesACodeThatIsNotOneOfOurs() {
    assertThatThrownBy(() -> service.update(CategoryScope.FOOD, "INVENTADA", "Inventada", "🎲"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.update(CategoryScope.SHOPPING, "INVENTADA", "Inventada", "🎲"))
        .isInstanceOf(NotFoundException.class);
  }

  /** The same word can name a category in both vocabularies without being the same category. */
  @Test
  void keepsTheTwoVocabulariesApart() {
    groups.seed(new FoodGroup("PROTEINA", "Proteína", "🍗", null, 2, true));
    displays.seed(new CategoryDisplay(CategoryScope.SHOPPING, "PROTEINAS", "Proteínas", "🍗"));

    service.update(CategoryScope.FOOD, "PROTEINA", "Proteínas y huevos", "🥚");

    assertThat(service.findAll(CategoryScope.SHOPPING))
        .singleElement()
        .satisfies(display -> assertThat(display.label()).isEqualTo("Proteínas"));
  }

  /** A food group code is not an aisle code, however familiar it looks. */
  @Test
  void willNotEditAFoodGroupThroughTheShoppingVocabulary() {
    groups.seed(new FoodGroup("PROTEINA", "Proteína", "🍗", null, 2, true));

    assertThatThrownBy(() -> service.update(CategoryScope.SHOPPING, "PROTEINA", "X", "🍗"))
        .isInstanceOf(NotFoundException.class);
  }

  /** An icon is decoration; a category is allowed to have none. A label is not optional. */
  @Test
  void allowsACategoryWithNoIcon() {
    groups.seed(new FoodGroup("GRASA", "Grasa", "🫒", null, 5, true));

    assertThat(service.update(CategoryScope.FOOD, "GRASA", "Grasa", null).icon()).isNull();
  }

  private static final class InMemoryDisplays implements CategoryDisplayRepository {
    private final Map<String, CategoryDisplay> rows = new LinkedHashMap<>();

    void seed(CategoryDisplay display) {
      rows.put(display.scope() + "/" + display.code(), display);
    }

    @Override
    public List<CategoryDisplay> findAll(CategoryScope scope) {
      List<CategoryDisplay> found = new ArrayList<>();
      for (CategoryDisplay row : rows.values()) {
        if (scope == null || row.scope() == scope) {
          found.add(row);
        }
      }
      return found;
    }

    @Override
    public Optional<CategoryDisplay> find(CategoryScope scope, String code) {
      return Optional.ofNullable(rows.get(scope + "/" + code));
    }

    @Override
    public void update(CategoryDisplay display) {
      seed(display);
    }
  }

  private static final class InMemoryGroups implements FoodGroupRepository {
    private final Map<String, FoodGroup> rows = new LinkedHashMap<>();

    void seed(FoodGroup group) {
      rows.put(group.id(), group);
    }

    @Override
    public List<FoodGroup> findAll() {
      return rows.values().stream()
          .sorted(java.util.Comparator.comparingInt(FoodGroup::sortOrder))
          .toList();
    }

    @Override
    public Optional<FoodGroup> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void update(FoodGroup group) {
      seed(group);
    }
  }
}

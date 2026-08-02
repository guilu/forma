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
 * Renaming and re-drawing a category (FOR-197). Hand-rolled fake, no Spring (ADR-007).
 *
 * <p>There is no create and no delete here on purpose: the set of categories is closed in the
 * domain enums and in the database's own CHECK constraints, and rows point at those codes. What an
 * admin owns is how each one is written and drawn.
 */
class CategoryDisplayServiceTest {

  private final InMemoryRepository repository = new InMemoryRepository();
  private final CategoryDisplayService service = new CategoryDisplayService(repository);

  @Test
  void listsBothVocabularies() {
    repository.seed(new CategoryDisplay(CategoryScope.FOOD, "PROTEINA", "Proteína", "🍗"));
    repository.seed(new CategoryDisplay(CategoryScope.SHOPPING, "OTROS", "Otros", "🛒"));

    assertThat(service.findAll(null)).hasSize(2);
    assertThat(service.findAll(CategoryScope.FOOD))
        .extracting(CategoryDisplay::code)
        .containsExactly("PROTEINA");
  }

  @Test
  void renamesACategoryAndChangesItsIcon() {
    repository.seed(new CategoryDisplay(CategoryScope.FOOD, "LACTEO", "Lácteo", "🥛"));

    CategoryDisplay updated =
        service.update(CategoryScope.FOOD, "LACTEO", "Lácteos y derivados", "🧀");

    assertThat(updated.label()).isEqualTo("Lácteos y derivados");
    assertThat(updated.icon()).isEqualTo("🧀");
  }

  /**
   * The code is the value stored on every row and checked by the database. An update that could
   * introduce one would be creating a category by the back door — and one nothing may ever be filed
   * under, since the CHECK would refuse it.
   */
  @Test
  void refusesACodeThatIsNotOneOfOurs() {
    assertThatThrownBy(() -> service.update(CategoryScope.FOOD, "INVENTADA", "Inventada", "🎲"))
        .isInstanceOf(NotFoundException.class);
  }

  /** The same word can name a category in both vocabularies without being the same category. */
  @Test
  void keepsTheTwoVocabulariesApart() {
    repository.seed(new CategoryDisplay(CategoryScope.FOOD, "PROTEINA", "Proteína", "🍗"));
    repository.seed(new CategoryDisplay(CategoryScope.SHOPPING, "PROTEINAS", "Proteínas", "🍗"));

    service.update(CategoryScope.FOOD, "PROTEINA", "Proteínas y huevos", "🥚");

    assertThat(service.findAll(CategoryScope.SHOPPING))
        .singleElement()
        .satisfies(display -> assertThat(display.label()).isEqualTo("Proteínas"));
  }

  /** An icon is decoration; a category is allowed to have none. A label is not optional. */
  @Test
  void allowsACategoryWithNoIcon() {
    repository.seed(new CategoryDisplay(CategoryScope.FOOD, "GRASA", "Grasa", "🫒"));

    assertThat(service.update(CategoryScope.FOOD, "GRASA", "Grasa", null).icon()).isNull();
  }

  private static final class InMemoryRepository implements CategoryDisplayRepository {
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
}

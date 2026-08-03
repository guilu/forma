package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Labelling foods (V50). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>A food's labels are replaced whole rather than added and removed one at a time: a form that
 * shows twelve checkboxes knows the complete answer when it is submitted, and reconciling it as a
 * diff would let two people editing at once each keep half of what the other did.
 */
class FoodTagServiceTest {

  private final InMemoryTags tags = new InMemoryTags();
  private final FoodCatalogService foods = SeededFoodCatalog.service();
  private final FoodTagService service = new FoodTagService(tags, foods);

  @Test
  void listsTheVocabularyInItsOwnOrder() {
    assertThat(service.allTags()).extracting(Tag::id).containsExactly("vegano", "fresco", "cena");
  }

  @Test
  void labelsAFoodAndReadsItBack() {
    service.setTagsOf("salad", List.of("vegano", "fresco"));

    assertThat(service.tagsOf("salad")).extracting(Tag::id).containsExactly("vegano", "fresco");
  }

  /** Replacing is replacing: what the form did not send is what somebody unticked. */
  @Test
  void replacesTheWholeSetRatherThanAddingToIt() {
    service.setTagsOf("salad", List.of("vegano", "fresco"));

    service.setTagsOf("salad", List.of("vegano"));

    assertThat(service.tagsOf("salad")).extracting(Tag::id).containsExactly("vegano");
  }

  @Test
  void clearsEveryLabelWhenNoneIsSent() {
    service.setTagsOf("salad", List.of("vegano"));

    service.setTagsOf("salad", List.of());

    assertThat(service.tagsOf("salad")).isEmpty();
  }

  /** Saying the same thing twice in one request is saying it once. */
  @Test
  void ignoresARepeatedLabelInTheSameRequest() {
    service.setTagsOf("salad", List.of("vegano", "vegano", "fresco"));

    assertThat(service.tagsOf("salad")).hasSize(2);
  }

  /**
   * A label nobody defined is a typo, not a request to invent one. Letting it through would grow
   * the vocabulary by accident and leave "Vegano" and "vegano" side by side.
   */
  @Test
  void refusesALabelNobodyDefined() {
    assertThatThrownBy(() -> service.setTagsOf("salad", List.of("inventado")))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("inventado");
  }

  /** And refuses the whole request rather than applying the half that was valid. */
  @Test
  void appliesNothingWhenOneLabelIsUnknown() {
    service.setTagsOf("salad", List.of("vegano"));

    assertThatThrownBy(() -> service.setTagsOf("salad", List.of("fresco", "inventado")))
        .isInstanceOf(ValidationException.class);

    assertThat(service.tagsOf("salad")).extracting(Tag::id).containsExactly("vegano");
  }

  @Test
  void refusesToLabelAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> service.setTagsOf("unicornio", List.of("vegano")))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unicornio");
    assertThatThrownBy(() -> service.tagsOf("unicornio")).isInstanceOf(ValidationException.class);
  }

  /** Labels come back in the vocabulary's order, not the order somebody happened to tick them. */
  @Test
  void readsLabelsBackInTheVocabularysOrder() {
    service.setTagsOf("salad", List.of("cena", "vegano"));

    assertThat(service.tagsOf("salad")).extracting(Tag::id).containsExactly("vegano", "cena");
  }

  private static final class InMemoryTags implements FoodTagRepository {
    private final Map<String, Tag> vocabulary =
        new LinkedHashMap<>(
            Map.of(
                "vegano", new Tag("vegano", "Vegano", 1, true),
                "fresco", new Tag("fresco", "Fresco", 6, true),
                "cena", new Tag("cena", "Cena", 11, true)));
    private final Map<String, Set<String>> labels = new LinkedHashMap<>();

    @Override
    public List<Tag> findAll() {
      List<Tag> all = new ArrayList<>(vocabulary.values());
      all.sort((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()));
      return all;
    }

    @Override
    public Optional<Tag> find(String id) {
      return Optional.ofNullable(vocabulary.get(id));
    }

    @Override
    public List<Tag> findByFood(String foodId) {
      Set<String> ids = labels.getOrDefault(foodId, Set.of());
      return findAll().stream().filter(tag -> ids.contains(tag.id())).toList();
    }

    @Override
    public void replaceTagsOf(String foodId, List<String> tagIds) {
      labels.put(foodId, new LinkedHashSet<>(tagIds));
    }
  }
}

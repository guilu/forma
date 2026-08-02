package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Store;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Suggests products from a store's own catalogue for a food in ours (FOR-194).
 *
 * <p>Suggests, and stops there. The admin picks one and the existing create endpoint stores it, so
 * nothing here writes: an automatic import would fill the catalog with rows nobody checked, and the
 * two fields that matter most — which food a product is, and which aisle it belongs in — are
 * precisely the ones the shop cannot tell us.
 *
 * <p>Matching is deliberately crude: normalise both names, drop the words every Spanish label
 * carries, and keep the products sharing a meaningful word with the food. It is a filter over a
 * shelf of thousands, not a decision; a wrong suggestion costs the admin a glance, and a missing
 * one costs them a manual entry they were making anyway.
 */
@Service
public class StoreProductImportService {

  /** As many as a person will actually read before giving up and typing it themselves. */
  private static final int MAX_SUGGESTIONS = 10;

  /**
   * Words too common in Spanish food labels to mean anything on their own. Without this, "Aceite de
   * oliva" matches every product with a "de" in it — which is most of the shop.
   */
  private static final Set<String> FILLER =
      Set.of("de", "del", "la", "el", "los", "las", "y", "con", "sin", "al", "en", "para", "a");

  /** Shorter than this and a word is noise, not a name ("0%", "kg"). */
  private static final int MIN_TOKEN_LENGTH = 3;

  private final FoodCatalogRepository foods;
  private final List<StoreCatalogSource> sources;

  public StoreProductImportService(FoodCatalogRepository foods, List<StoreCatalogSource> sources) {
    this.foods = foods;
    this.sources = sources;
  }

  /**
   * The store's products worth considering for {@code foodId}, best first.
   *
   * @throws NotFoundException when no food has that id, or when no source speaks for that chain —
   *     an empty list would claim the shop stocks nothing, which is a different answer
   * @throws StoreCatalogUnavailableException when the store cannot be reached
   */
  public List<ImportableProduct> suggestionsFor(String foodId, Store store) {
    CatalogFood food =
        foods
            .findById(foodId)
            .orElseThrow(() -> new NotFoundException("No existe el alimento: " + foodId));
    StoreCatalogSource source =
        sources.stream()
            .filter(candidate -> candidate.store() == store)
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException("No hay catálogo disponible para: " + store.name()));

    Set<String> wanted = meaningfulTokens(food.name());
    if (wanted.isEmpty()) {
      return List.of();
    }
    return source.products().stream()
        .map(product -> new Scored(product, score(wanted, product.name())))
        .filter(scored -> scored.score() > 0)
        // Best match first; among equals the shorter name, which is the plainer
        // product rather than the one with three qualifiers after it.
        .sorted(
            Comparator.comparingInt(Scored::score)
                .reversed()
                .thenComparingInt(scored -> scored.product().name().length()))
        .limit(MAX_SUGGESTIONS)
        .map(Scored::product)
        .toList();
  }

  private record Scored(ImportableProduct product, int score) {}

  /** How many of the food's meaningful words the shelf name carries. */
  private static int score(Set<String> wanted, String productName) {
    Set<String> found = meaningfulTokens(productName);
    return (int) wanted.stream().filter(found::contains).count();
  }

  /**
   * Lower case, accents stripped, filler and short words dropped.
   *
   * <p>Accents and case are how a shelf label differs from a catalog entry, not what makes them
   * different products: "Plátano" has to find "PLATANOS DE CANARIAS".
   */
  private static Set<String> meaningfulTokens(String text) {
    String normalised =
        Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
    return Arrays.stream(normalised.split("[^a-z0-9]+"))
        .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
        .filter(token -> !FILLER.contains(token))
        // Crude plural stripping, so "platano" meets "platanos". Spanish plurals
        // are almost always a trailing -s or -es on these labels.
        .map(token -> token.endsWith("es") ? token.substring(0, token.length() - 2) : token)
        .map(token -> token.endsWith("s") ? token.substring(0, token.length() - 1) : token)
        .collect(Collectors.toSet());
  }
}

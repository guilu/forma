package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Turning a shop's aisles into rows (V46). Plain JUnit 5 + AssertJ (ADR-007).
 *
 * <p>A shop hands over a nested structure; the table holds a flat list with a parent pointer and a
 * stored {@code level}. This is the piece that converts one into the other, and the reason it is
 * tested on its own is that {@code level} is derived data: nothing in the database can check it
 * against the parent chain, so it has to be right here or it is wrong everywhere.
 */
class StoreCategoryTreeTest {

  private static StoreCategoryNode node(String externalId, String name, StoreCategoryNode... kids) {
    return new StoreCategoryNode(externalId, name, List.of(kids));
  }

  @Test
  void flattensATreeDepthFirstWithEachRowPointingAtItsParent() {
    List<StoreCategory> rows =
        StoreCategoryTree.flatten(
            "MERCADONA",
            List.of(
                node(
                    "112",
                    "Cereales y galletas",
                    node("113", "Avena y cereales", node("114", "Copos")))));

    assertThat(rows).extracting(StoreCategory::externalId).containsExactly("112", "113", "114");
    assertThat(rows).extracting(StoreCategory::level).containsExactly(0, 1, 2);
    assertThat(rows.get(0).parentId()).isNull();
    assertThat(rows.get(1).parentId()).isEqualTo(rows.get(0).id());
    assertThat(rows.get(2).parentId()).isEqualTo(rows.get(1).id());
  }

  /**
   * The id is built from the shop and the shop's own id for the aisle, so crawling twice writes the
   * same rows rather than a second copy of the tree.
   */
  @Test
  void buildsTheSameIdForTheSameAisleEveryTime() {
    List<StoreCategory> first = StoreCategoryTree.flatten("MERCADONA", List.of(node("112", "X")));
    List<StoreCategory> again =
        StoreCategoryTree.flatten("MERCADONA", List.of(node("112", "X renombrado")));

    assertThat(first.get(0).id()).isEqualTo(again.get(0).id());
    assertThat(first.get(0).id()).contains("MERCADONA").contains("112");
  }

  /** Two shops number their aisles independently, so the same aisle id must not collide. */
  @Test
  void keepsTwoShopsAislesApart() {
    String mercadona =
        StoreCategoryTree.flatten("MERCADONA", List.of(node("112", "X"))).get(0).id();
    String carrefour =
        StoreCategoryTree.flatten("CARREFOUR", List.of(node("112", "X"))).get(0).id();

    assertThat(mercadona).isNotEqualTo(carrefour);
  }

  /** Siblings keep the order the shop listed them in; alphabetising would be our opinion. */
  @Test
  void keepsSiblingsInTheOrderTheShopGaveThem() {
    List<StoreCategory> rows =
        StoreCategoryTree.flatten(
            "MERCADONA", List.of(node("2", "Zumos"), node("1", "Aceites"), node("3", "Bebidas")));

    assertThat(rows).extracting(StoreCategory::name).containsExactly("Zumos", "Aceites", "Bebidas");
    assertThat(rows).extracting(StoreCategory::sortOrder).containsExactly(0, 1, 2);
  }

  @Test
  void slugsTheNameForReadingAndRouting() {
    List<StoreCategory> rows =
        StoreCategoryTree.flatten("MERCADONA", List.of(node("1", "Frutos secos y fruta desecada")));

    assertThat(rows.get(0).slug()).isEqualTo("frutos-secos-y-fruta-desecada");
  }

  /** Spanish aisle names are full of accents, and a slug with them in is no slug. */
  @Test
  void stripsAccentsAndPunctuationFromTheSlug() {
    List<StoreCategory> rows =
        StoreCategoryTree.flatten(
            "MERCADONA",
            List.of(node("1", "Leche, café e infusiones"), node("2", "Pizzas   y  pastas")));

    assertThat(rows)
        .extracting(StoreCategory::slug)
        .containsExactly("leche-cafe-e-infusiones", "pizzas-y-pastas");
  }

  /**
   * A shop that repeats an aisle id inside one crawl is telling us two things about one row. Taking
   * the last would silently drop a whole branch, so it fails instead.
   */
  @Test
  void refusesAShopThatRepeatsAnAisleId() {
    assertThatThrownBy(
            () ->
                StoreCategoryTree.flatten(
                    "MERCADONA", List.of(node("112", "Cereales"), node("112", "Otra cosa"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("112");
  }

  @Test
  void acceptsAShopWithNoAislesAtAll() {
    assertThat(StoreCategoryTree.flatten("OTRAS", List.of())).isEmpty();
  }
}

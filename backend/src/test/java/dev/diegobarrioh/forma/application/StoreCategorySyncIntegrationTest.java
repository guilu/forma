package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.adapter.mercadona.MercadonaHttpTransport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Syncing Mercadona's aisles all the way into the database (V46).
 *
 * <p>Every piece of this is covered on its own — the adapter parses, {@code StoreCategoryTree}
 * flattens, {@code StoreCategoryService} decides what to write, the JDBC adapter writes. What none
 * of those can show is that the pieces fit: that the ids the flattening invents satisfy the foreign
 * key, that parents really do land before their children, and that a real {@code level} column
 * accepts what the tree computed.
 *
 * <p>The shop is faked at the transport, the lowest seam there is, so everything above it is the
 * production wiring.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoreCategorySyncIntegrationTest {

  private static final String INDEX =
      """
      {"results":[
        {"id":12,"name":"Aceite, especias y salsas","categories":[
          {"id":112,"name":"Aceite, vinagre y sal","published":true},
          {"id":115,"name":"Especias","published":true}]},
        {"id":19,"name":"Cereales y galletas","categories":[
          {"id":200,"name":"Cereales","published":true}]}]}
      """;

  @TestConfiguration
  static class FakeShop {
    @Bean
    @Primary
    MercadonaHttpTransport transport() {
      return url -> INDEX;
    }
  }

  @Autowired private StoreCategoryService service;

  @Test
  void writesTheShopsTreeAndReadsItBackInOrder() {
    int written = service.sync("MERCADONA");

    assertThat(written).isEqualTo(5); // 2 headings + 3 shelves

    List<StoreCategory> stored = service.findByStore("MERCADONA");

    // Parents before children, which is what makes the list safe to insert and simple to render.
    assertThat(stored).extracting(StoreCategory::level).isSorted();
    assertThat(stored.stream().filter(c -> c.level() == 0).toList())
        .extracting(StoreCategory::name)
        .containsExactly("Aceite, especias y salsas", "Cereales y galletas");

    StoreCategory heading =
        stored.stream().filter(c -> c.externalId().equals("12")).findFirst().orElseThrow();
    assertThat(heading.parentId()).isNull();
    assertThat(heading.slug()).isEqualTo("aceite-especias-y-salsas");

    // The foreign key holds: a child's parentId is a row that really exists.
    assertThat(stored.stream().filter(c -> c.externalId().equals("112")).findFirst().orElseThrow())
        .satisfies(
            shelf -> {
              assertThat(shelf.parentId()).isEqualTo(heading.id());
              assertThat(shelf.level()).isEqualTo(1);
            });
  }

  /** Syncing twice is not an error and does not double the tree — the ids are deterministic. */
  @Test
  void syncingTwiceLeavesTheSameTree() {
    service.sync("MERCADONA");
    service.sync("MERCADONA");

    assertThat(service.findByStore("MERCADONA")).hasSize(5);
  }
}

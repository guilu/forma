package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.Store;
import dev.diegobarrioh.forma.application.StoreCatalogSource;
import dev.diegobarrioh.forma.application.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Every catalogue source speaks for a chain that exists.
 *
 * <p>A source declares which {@code store} row it serves as a plain string, because the chains are
 * data and the sources are code — V45 separated the two on purpose. The cost of that string is that
 * a typo, or a chain someone retired, would leave a source answering for nothing: imports would
 * fail with "no catalogue available" for a shop whose adapter is right there. Nothing but this test
 * connects the two sets, so it is what keeps them honest.
 */
@SpringBootTest
@ActiveProfiles("test")
class StoreCatalogSourcesMatchAStoreTest {

  @Autowired private List<StoreCatalogSource> sources;
  @Autowired private StoreRepository stores;

  @Test
  void everySourceDeclaresAChainThatExists() {
    List<String> known = stores.findAll().stream().map(Store::id).toList();

    assertThat(sources).isNotEmpty();
    assertThat(sources)
        .allSatisfy(
            source ->
                assertThat(known)
                    .as("store id declared by %s", source.getClass().getSimpleName())
                    .contains(source.store()));
  }

  /** Two sources for one chain would make which of them answers an accident of bean ordering. */
  @Test
  void noTwoSourcesSpeakForTheSameChain() {
    assertThat(sources.stream().map(StoreCatalogSource::store).toList()).doesNotHaveDuplicates();
  }
}

package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.ShoppingBudget;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShoppingCostRecommendationService} (FOR-150 rule 6): reads the active
 * shopping list's FOR-152 budget and delegates to the pure {@link
 * dev.diegobarrioh.forma.domain.ShoppingCostRules} evaluator.
 */
class ShoppingCostRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  private final ShoppingListRepository listRepository = mock(ShoppingListRepository.class);
  private final ShoppingBudgetService budgetService = mock(ShoppingBudgetService.class);
  private final ShoppingCostRecommendationService service =
      new ShoppingCostRecommendationService(listRepository, budgetService, FIXED);

  private static ActiveShoppingList activeList() {
    return new ActiveShoppingList(
        "list-1", LocalDate.of(2026, 7, 6), ShoppingListStatus.ACTIVE, null, List.of(), NOW);
  }

  @Test
  void producesARecommendationWhenTheActiveListIsOverThreshold() {
    when(listRepository.findActive()).thenReturn(Optional.of(activeList()));
    when(budgetService.budgetFor(any()))
        .thenReturn(
            new ShoppingBudget(
                new BigDecimal("135.00"),
                new BigDecimal("135.00"),
                new BigDecimal("120.00"),
                true));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    assertThat(recs.get(0).category()).isEqualTo(RecommendationCategory.SHOPPING);
    assertThat(recs.get(0).createdAt()).isEqualTo(NOW);
  }

  @Test
  void producesNothingWhenUnderThreshold() {
    when(listRepository.findActive()).thenReturn(Optional.of(activeList()));
    when(budgetService.budgetFor(any()))
        .thenReturn(
            new ShoppingBudget(
                new BigDecimal("90.00"), new BigDecimal("90.00"), new BigDecimal("120.00"), false));

    assertThat(service.currentRecommendations()).isEmpty();
  }

  @Test
  void producesNothingWhenThereIsNoActiveList() {
    when(listRepository.findActive()).thenReturn(Optional.empty());

    assertThat(service.currentRecommendations()).isEmpty();
  }
}

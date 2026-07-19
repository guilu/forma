package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.ShoppingCostRules;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the shopping cost recommendation (FOR-150 rule 6).
 *
 * <p>Reads the active shopping list via {@link ShoppingListRepository} and its FOR-152 budget via
 * {@link ShoppingBudgetService}, then delegates to the pure {@link ShoppingCostRules} domain
 * evaluator; stamps any recommendation with {@link Instant#now(Clock)} from the injected clock.
 * Computed on demand — no persistence. An absent active list (spec edge case, e.g. before FOR-152's
 * data exists) yields no recommendation rather than an error, mirroring the other recommendation
 * services' fail-safe behavior.
 */
@Service
public class ShoppingCostRecommendationService {

  private final ShoppingListRepository listRepository;
  private final ShoppingBudgetService budgetService;
  private final Clock clock;

  public ShoppingCostRecommendationService(
      ShoppingListRepository listRepository, ShoppingBudgetService budgetService, Clock clock) {
    this.listRepository = listRepository;
    this.budgetService = budgetService;
    this.clock = clock;
  }

  /** Evaluates the active shopping list's budget for the over-threshold cost signal. */
  public List<Recommendation> currentRecommendations() {
    return listRepository
        .findActive()
        .map(
            active ->
                ShoppingCostRules.evaluate(
                    budgetService.budgetFor(active.toDomain()), Instant.now(clock)))
        .orElseGet(List::of);
  }
}

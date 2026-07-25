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
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V33): resolves the caller's account id via
 * {@link CurrentUserProvider} so the active list read is scoped to the caller's own lists.
 */
@Service
public class ShoppingCostRecommendationService {

  private final ShoppingListRepository listRepository;
  private final ShoppingBudgetService budgetService;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public ShoppingCostRecommendationService(
      ShoppingListRepository listRepository,
      ShoppingBudgetService budgetService,
      Clock clock,
      CurrentUserProvider currentUserProvider) {
    this.listRepository = listRepository;
    this.budgetService = budgetService;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
  }

  /** Evaluates the caller's active shopping list's budget for the over-threshold cost signal. */
  public List<Recommendation> currentRecommendations() {
    return listRepository
        .findActive(currentUserProvider.currentUserId())
        .map(
            active ->
                ShoppingCostRules.evaluate(
                    budgetService.budgetFor(active.toDomain()), Instant.now(clock)))
        .orElseGet(List::of);
  }
}

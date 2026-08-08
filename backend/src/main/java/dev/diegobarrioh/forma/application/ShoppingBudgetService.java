package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingBudget;
import dev.diegobarrioh.forma.domain.ShoppingBudgetCalculator;
import dev.diegobarrioh.forma.domain.ShoppingList;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case computing a shopping list's budget (FOR-38).
 *
 * <p>Resolves current product prices from the FOR-36 {@link ShoppingProductRepository} and
 * delegates the arithmetic to the pure {@link ShoppingBudgetCalculator}. Because it reads current
 * prices, the budget reflects price changes; computed on demand, no persisted budget. Mirrors the
 * FOR-21/FOR-28 service pattern.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V32): resolves the caller's account id via
 * {@link CurrentUserProvider} so the budget is computed from the caller's own product catalog.
 */
@Service
public class ShoppingBudgetService {

  private final ShoppingProductRepository productRepository;
  private final CurrentUserProvider currentUserProvider;

  public ShoppingBudgetService(
      ShoppingProductRepository productRepository, CurrentUserProvider currentUserProvider) {
    this.productRepository = productRepository;
    this.currentUserProvider = currentUserProvider;
  }

  /** Computes the weekly + monthly budget for a list using current product prices. */
  public ShoppingBudget budgetFor(ShoppingList list) {
    Map<String, BigDecimal> unitPriceById =
        productRepository.findAllByOwner(currentUserProvider.currentUserId()).stream()
            .filter(stored -> stored.product().estimatedPriceEur() != null)
            .collect(
                Collectors.toMap(
                    StoredShoppingProduct::id, stored -> stored.product().estimatedPriceEur()));
    return ShoppingBudgetCalculator.budget(list, unitPriceById);
  }
}

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
 */
@Service
public class ShoppingBudgetService {

  private final ShoppingProductRepository productRepository;

  public ShoppingBudgetService(ShoppingProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  /** Computes the weekly + monthly budget for a list using current product prices. */
  public ShoppingBudget budgetFor(ShoppingList list) {
    Map<String, BigDecimal> unitPriceById =
        productRepository.findAll().stream()
            .collect(
                Collectors.toMap(
                    StoredShoppingProduct::id, stored -> stored.product().estimatedPriceEur()));
    return ShoppingBudgetCalculator.budget(list, unitPriceById);
  }
}

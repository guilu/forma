package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.application.ShoppingListView;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/shopping/list} (FOR-39): the weekly checklist with resolved
 * product names, ids, categories (FOR-106), unit/servings (FOR-108), a provider link-out {@code
 * productUrl} (FOR-109), and the budget, plus the list's {@code generatedAt} timestamp (FOR-108).
 * Also reused as the response for {@code POST .../regenerate} (FOR-109), since a regenerate
 * rebuilds the whole list. Delivery read model, distinct from the application view (ADR-005).
 */
public record ShoppingListResponse(
    String weekStartDate, String status, List<Item> items, Budget budget, String generatedAt) {

  public record Item(
      String id,
      String productId,
      String productName,
      String category,
      int quantity,
      BigDecimal estimatedCostEur,
      boolean checked,
      String unit,
      Integer servings,
      String productUrl) {}

  /**
   * @param weeklyThresholdEur the FOR-152 plan cost-threshold constant (&lt;120 €/sem)
   * @param overThreshold whether {@code weeklyEur} exceeds {@code weeklyThresholdEur} (strictly),
   *     consumed by the dashboard signal (frontend batch, deferred) and FOR-150 rule 6
   */
  public record Budget(
      BigDecimal weeklyEur,
      BigDecimal monthlyEur,
      BigDecimal weeklyThresholdEur,
      boolean overThreshold) {}

  /** Maps the application view to its API read model. */
  public static ShoppingListResponse from(ShoppingListView view) {
    List<Item> items =
        view.items().stream()
            .map(
                entry ->
                    new Item(
                        entry.id(),
                        entry.productId(),
                        entry.productName(),
                        entry.category().name(),
                        entry.quantity(),
                        entry.estimatedCostEur(),
                        entry.checked(),
                        entry.unit().name(),
                        entry.servings(),
                        entry.productUrl()))
            .toList();
    Budget budget =
        new Budget(
            view.budget().weeklyEur(),
            view.budget().monthlyEur(),
            view.budget().weeklyThresholdEur(),
            view.budget().overThreshold());
    return new ShoppingListResponse(
        view.weekStartDate().toString(),
        view.status().name(),
        items,
        budget,
        view.generatedAt().toString());
  }
}

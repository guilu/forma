package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.application.ShoppingListView;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/shopping/list} (FOR-39): the weekly checklist with resolved
 * product names and the budget. Delivery read model, distinct from the application view (ADR-005).
 */
public record ShoppingListResponse(
    String weekStartDate, String status, List<Item> items, Budget budget) {

  public record Item(
      String id, String productName, int quantity, BigDecimal estimatedCostEur, boolean checked) {}

  public record Budget(BigDecimal weeklyEur, BigDecimal monthlyEur) {}

  /** Maps the application view to its API read model. */
  public static ShoppingListResponse from(ShoppingListView view) {
    List<Item> items =
        view.items().stream()
            .map(
                entry ->
                    new Item(
                        entry.id(),
                        entry.productName(),
                        entry.quantity(),
                        entry.estimatedCostEur(),
                        entry.checked()))
            .toList();
    Budget budget = new Budget(view.budget().weeklyEur(), view.budget().monthlyEur());
    return new ShoppingListResponse(
        view.weekStartDate().toString(), view.status().name(), items, budget);
  }
}

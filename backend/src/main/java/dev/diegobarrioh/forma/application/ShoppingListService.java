package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.ShoppingListView.Entry;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for the weekly shopping list checklist (FOR-39).
 *
 * <p>Reads the active list via {@link ShoppingListRepository}, resolves product names from the
 * FOR-36 {@link ShoppingProductRepository}, and computes the budget via {@link
 * ShoppingBudgetService} (FOR-38) into a single {@link ShoppingListView}. Also toggles an item's
 * checked state. An absent list or unknown item yields {@link NotFoundException} → 404.
 */
@Service
public class ShoppingListService {

  private final ShoppingListRepository listRepository;
  private final ShoppingProductRepository productRepository;
  private final ShoppingBudgetService budgetService;

  public ShoppingListService(
      ShoppingListRepository listRepository,
      ShoppingProductRepository productRepository,
      ShoppingBudgetService budgetService) {
    this.listRepository = listRepository;
    this.productRepository = productRepository;
    this.budgetService = budgetService;
  }

  /** The current week's checklist with resolved names and budget. */
  public ShoppingListView currentView() {
    ActiveShoppingList active =
        listRepository
            .findActive()
            .orElseThrow(() -> new NotFoundException("No hay lista de compra activa"));

    Map<String, String> namesById =
        productRepository.findAll().stream()
            .collect(
                Collectors.toMap(StoredShoppingProduct::id, stored -> stored.product().name()));

    var entries =
        active.items().stream()
            .map(
                stored ->
                    new Entry(
                        stored.id(),
                        namesById.getOrDefault(
                            stored.item().productId(), stored.item().productId()),
                        stored.item().quantity(),
                        stored.item().estimatedCostEur(),
                        stored.item().checked()))
            .toList();

    return new ShoppingListView(
        active.weekStartDate(),
        active.status(),
        entries,
        budgetService.budgetFor(active.toDomain()));
  }

  /**
   * Toggles an item's checked state.
   *
   * @throws NotFoundException if no item has the given id
   */
  public StoredShoppingListItem setChecked(String itemId, boolean checked) {
    return listRepository
        .setChecked(itemId, checked)
        .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));
  }
}

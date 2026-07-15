package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.ShoppingListView.Entry;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for the weekly shopping list checklist (FOR-39).
 *
 * <p>Reads the active list via {@link ShoppingListRepository}, resolves product names and
 * categories (FOR-106) from the FOR-36 {@link ShoppingProductRepository}, and computes the budget
 * via {@link ShoppingBudgetService} (FOR-38) into a single {@link ShoppingListView}. Also toggles
 * an item's checked state. An absent list or unknown item yields {@link NotFoundException} → 404.
 * An item whose product id no longer resolves falls back to the raw id as its name and {@link
 * ShoppingCategory#OTROS} as its category (no crash).
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

  /** The current week's checklist with resolved names, categories and budget. */
  public ShoppingListView currentView() {
    ActiveShoppingList active =
        listRepository
            .findActive()
            .orElseThrow(() -> new NotFoundException("No hay lista de compra activa"));

    Map<String, StoredShoppingProduct> productsById =
        productRepository.findAll().stream()
            .collect(Collectors.toMap(StoredShoppingProduct::id, stored -> stored));

    var entries =
        active.items().stream()
            .map(
                stored -> {
                  String productId = stored.item().productId();
                  StoredShoppingProduct product = productsById.get(productId);
                  String productName = product == null ? productId : product.product().name();
                  ShoppingCategory category =
                      product == null ? ShoppingCategory.OTROS : product.product().category();
                  // Servings only surface for items whose product is genuinely linked to a
                  // nutrition food (FOR-108) — never fabricated for non-food/unresolved items.
                  boolean linkedToFood =
                      product != null && product.product().linkedFoodItemId() != null;
                  Integer servings = linkedToFood ? stored.item().servings() : null;
                  return new Entry(
                      stored.id(),
                      productId,
                      productName,
                      category,
                      stored.item().quantity(),
                      stored.item().estimatedCostEur(),
                      stored.item().checked(),
                      stored.item().unit(),
                      servings);
                })
            .toList();

    return new ShoppingListView(
        active.weekStartDate(),
        active.status(),
        entries,
        budgetService.budgetFor(active.toDomain()),
        active.generatedAt());
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

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.ShoppingListView.Entry;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use case for the weekly shopping list checklist (FOR-39) and its FOR-109 write
 * commands.
 *
 * <p>Reads the active list via {@link ShoppingListRepository}, resolves product names, categories
 * (FOR-106) and link-out URLs (FOR-109) from the FOR-36 {@link ShoppingProductRepository}, and
 * computes the budget via {@link ShoppingBudgetService} (FOR-38) into a single {@link
 * ShoppingListView}. Also toggles an item's checked state, regenerates the list, and edits an
 * item's quantity. An absent list or unknown item yields {@link NotFoundException} → 404. An item
 * whose product id no longer resolves falls back to the raw id as its name and {@link
 * ShoppingCategory#OTROS} as its category (no crash). The entry's line cost is likewise derived
 * live from the product's current {@code estimatedPriceEur} × quantity (mirroring {@link
 * #updateQuantity} and {@link dev.diegobarrioh.forma.domain.ShoppingBudgetCalculator}) so an edited
 * product price is reflected immediately instead of the stored snapshot going stale; falling back
 * to that stored snapshot only when the product no longer resolves.
 *
 * <p><strong>Regenerate (FOR-109):</strong> the repository has no algorithmic "generate a list from
 * nutrition templates" logic — FOR-37's own spec explicitly deferred that ("automatic generation
 * from nutrition templates is a later concern") and the only list ever created was seeded via a
 * Flyway migration INSERT ({@code V5__shopping_lists.sql}), not runtime code. Per AGENTS.md's
 * repository-priority rule, {@link #regenerate()} does not invent that algorithm. Instead it
 * rebuilds the active list's items from the current FOR-36 product catalog: one item per product,
 * quantity 1, cost = the product's current {@code estimatedPriceEur}, checked reset to {@code
 * false} (per the spec's Open Question recommendation "recommend always resetting for MVP
 * simplicity"). {@code weekStartDate}/{@code status} are left unchanged; only the items and {@code
 * generatedAt} are replaced.
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

  /** The current week's checklist with resolved names, categories, link-outs and budget. */
  public ShoppingListView currentView() {
    ActiveShoppingList active =
        listRepository
            .findActive()
            .orElseThrow(() -> new NotFoundException("No hay lista de compra activa"));

    Map<String, StoredShoppingProduct> productsById = productsById();

    var entries = active.items().stream().map(stored -> toEntry(stored, productsById)).toList();

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

  /**
   * Rebuilds the active list from the current product catalog (see class javadoc for why this is
   * not a nutrition-based generation algorithm), resets checked state and stamps a new {@code
   * generatedAt}. An empty product catalog produces a valid, empty list (spec edge case).
   *
   * @throws NotFoundException if there is no active list to regenerate
   */
  public ShoppingListView regenerate() {
    var freshItems =
        productRepository.findAll().stream()
            .map(
                product ->
                    new ShoppingListItem(
                        product.id(),
                        1,
                        product.product().estimatedPriceEur(),
                        false,
                        ShoppingUnit.UD,
                        null))
            .toList();
    listRepository
        .regenerate(freshItems, Instant.now())
        .orElseThrow(() -> new NotFoundException("No hay lista de compra activa"));
    return currentView();
  }

  /**
   * Edits an item's quantity, recalculating {@code estimatedCostEur} from the product's current
   * stored {@code estimatedPriceEur} (mirrors {@link
   * dev.diegobarrioh.forma.domain.ShoppingBudgetCalculator}, which uses the same field for its
   * price × quantity math). Editing to the item's current quantity is idempotent — it simply
   * recomputes the same cost.
   *
   * @throws NotFoundException if no item has the given id, or if the item's product id no longer
   *     resolves (rejected rather than fabricating a cost from nothing, per spec edge case)
   */
  public StoredShoppingListItem updateQuantity(String itemId, int quantity) {
    StoredShoppingListItem existing =
        listRepository
            .findItem(itemId)
            .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));

    StoredShoppingProduct product = productsById().get(existing.item().productId());
    if (product == null) {
      throw new NotFoundException(
          "El producto del artículo ya no existe: " + existing.item().productId());
    }

    BigDecimal newCost =
        product
            .product()
            .estimatedPriceEur()
            .multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);

    return listRepository
        .updateQuantity(itemId, quantity, newCost)
        .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));
  }

  private Map<String, StoredShoppingProduct> productsById() {
    return productRepository.findAll().stream()
        .collect(Collectors.toMap(StoredShoppingProduct::id, stored -> stored));
  }

  private Entry toEntry(
      StoredShoppingListItem stored, Map<String, StoredShoppingProduct> productsById) {
    String productId = stored.item().productId();
    StoredShoppingProduct product = productsById.get(productId);
    String productName = product == null ? productId : product.product().name();
    ShoppingCategory category =
        product == null ? ShoppingCategory.OTROS : product.product().category();
    // Servings only surface for items whose product is genuinely linked to a nutrition food
    // (FOR-108) — never fabricated for non-food/unresolved items.
    boolean linkedToFood = product != null && product.product().linkedFoodItemId() != null;
    Integer servings = linkedToFood ? stored.item().servings() : null;
    // Link-out URL (FOR-109): resolved the same way as name/category — null when the product no
    // longer resolves or genuinely has no stored URL, never a broken link.
    String productUrl = product == null ? null : product.product().url();
    // Line cost is derived LIVE from the product's current price (mirrors updateQuantity() and
    // ShoppingBudgetCalculator, which already do this), so an edited product price is reflected
    // immediately instead of showing a stale stored snapshot. Falls back to the stored snapshot
    // when the product no longer resolves, same as name/category/url above (no crash, no
    // fabricated cost).
    BigDecimal estimatedCostEur =
        product == null
            ? stored.item().estimatedCostEur()
            : product
                .product()
                .estimatedPriceEur()
                .multiply(BigDecimal.valueOf(stored.item().quantity()))
                .setScale(2, RoundingMode.HALF_UP);
    return new Entry(
        stored.id(),
        productId,
        productName,
        category,
        stored.item().quantity(),
        estimatedCostEur,
        stored.item().checked(),
        stored.item().unit(),
        servings,
        productUrl);
  }
}

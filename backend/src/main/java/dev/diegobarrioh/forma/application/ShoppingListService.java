package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.ShoppingListView.Entry;
import dev.diegobarrioh.forma.domain.GroceryQuantityCalculator;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingList;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V33): this "gap table" service had ZERO
 * owner-scoping before this slice. Every use case now resolves the caller's account id via {@link
 * CurrentUserProvider} and passes it to both {@link ShoppingListRepository} and {@link
 * ShoppingProductRepository} on every call. {@code shopping_list_items} has no {@code user_id} of
 * its own — item operations are scoped through their owning list's {@code user_id}.
 *
 * <p><strong>Where a list comes from (FOR-192):</strong> regenerate now seeds the caller's own
 * product entries from the global {@code store_product} catalog before rebuilding, so a list is
 * built by REFERENCE to shared reference data rather than from whatever each account happened to
 * type in. Entries the account already has are untouched, including any price it overrode.
 *
 * <p><strong>Regenerate (FOR-109):</strong> when an active nutrition plan resolves to food items,
 * regeneration aggregates their required grams and asks for enough whole catalog packages to cover
 * them. Until the remaining plan-to-shopping rules are specified, a plan with no resolvable food
 * items falls back to the previous catalog behavior: one item per product, quantity 1 and the
 * product's current estimated price. In either path checked state resets, {@code
 * weekStartDate}/{@code status} stay unchanged, and the items plus {@code generatedAt} are
 * replaced.
 */
@Service
public class ShoppingListService {

  private final ShoppingListRepository listRepository;
  private final ShoppingProductRepository productRepository;
  private final ShoppingBudgetService budgetService;
  private final CurrentUserProvider currentUserProvider;
  private final StoreProductRepository storeProductRepository;
  private final PlannedWeekSource plannedWeek;

  public ShoppingListService(
      ShoppingListRepository listRepository,
      ShoppingProductRepository productRepository,
      ShoppingBudgetService budgetService,
      CurrentUserProvider currentUserProvider,
      StoreProductRepository storeProductRepository,
      PlannedWeekSource plannedWeek) {
    this.listRepository = listRepository;
    this.productRepository = productRepository;
    this.budgetService = budgetService;
    this.currentUserProvider = currentUserProvider;
    this.storeProductRepository = storeProductRepository;
    this.plannedWeek = plannedWeek;
  }

  /**
   * The caller's current week's checklist with resolved names, categories, link-outs and budget.
   */
  public ShoppingListView currentView() {
    UUID userId = currentUserProvider.currentUserId();
    Optional<ActiveShoppingList> current = listRepository.findActive(userId);
    if (current.isEmpty()) {
      return emptyWeek();
    }
    ActiveShoppingList active = current.get();

    Map<String, StoredShoppingProduct> productsById = productsById(userId);

    var entries = active.items().stream().map(stored -> toEntry(stored, productsById)).toList();

    return new ShoppingListView(
        active.weekStartDate(),
        active.status(),
        entries,
        budgetService.budgetFor(active.toDomain()),
        active.generatedAt());
  }

  /**
   * The week of somebody who has no list yet.
   *
   * <p>Not a 404, and the difference matters on screen: a missing resource made the page show "no
   * se pudo cargar", which said something had broken when nothing had. An account simply has no
   * list until it generates one — an ordinary state, and one the screen already has an empty state
   * for, with the button that fixes it.
   */
  private ShoppingListView emptyWeek() {
    // UTC como el repositorio cuando crea la lista de verdad: dos husos distintos pondrían la
    // semana vacía en un lunes y la creada en otro.
    Instant now = Instant.now();
    LocalDate monday = LocalDate.ofInstant(now, ZoneOffset.UTC).with(DayOfWeek.MONDAY);
    ShoppingList empty = new ShoppingList(monday, ShoppingListStatus.ACTIVE, List.of(), null, now);
    return new ShoppingListView(
        monday, ShoppingListStatus.ACTIVE, List.of(), budgetService.budgetFor(empty), null);
  }

  /**
   * Toggles an item's checked state.
   *
   * @throws NotFoundException if no item has the given id
   */
  public StoredShoppingListItem setChecked(String itemId, boolean checked) {
    return listRepository
        .setChecked(currentUserProvider.currentUserId(), itemId, checked)
        .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));
  }

  /**
   * Rebuilds the active list from the active nutrition plan when it yields food items, otherwise
   * from the current product catalog. Resets checked state and stamps a new {@code generatedAt}. An
   * empty product catalog produces a valid, empty list (spec edge case).
   *
   * @throws NotFoundException if there is no active list to regenerate
   */
  public ShoppingListView regenerate() {
    UUID userId = currentUserProvider.currentUserId();
    // The account's entries come from the shared catalog (FOR-192): give it one
    // for every catalog product it does not have yet, then rebuild from its own
    // entries as before. A new account owns nothing, so without this step it
    // would regenerate into an empty list for ever.
    //
    // Every chain at once, because there is nowhere yet to record which
    // supermarket a person shops at. When that preference exists this is the
    // line that reads it.
    List<String> catalogIds =
        storeProductRepository.findAll(null).stream().map(CatalogStoreProduct::id).toList();
    productRepository.addMissingCatalogReferences(userId, catalogIds);

    List<ShoppingListItem> fromPlan = itemsFromActivePlan(userId);
    var freshItems =
        !fromPlan.isEmpty()
            ? fromPlan
            : productRepository.findAllByOwner(userId).stream()
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
        .regenerate(userId, freshItems, Instant.now())
        .orElseThrow(() -> new NotFoundException("No hay lista de compra activa"));
    return currentView();
  }

  /**
   * Lo que hay que comprar para la semana del plan.
   *
   * <p>Suma los gramos que cada alimento aparece a lo largo de la semana natural en curso del plan
   * activo, y por cada uno pide los envases que hacen falta para cubrirlos. La semana entera de una
   * vez, que es como se hace la compra.
   *
   * <p><b>Lo que el plan pide y la tienda no tiene catalogado entra igualmente, sin precio.</b> Es
   * el punto: la lista sirve para saber qué comprar, y omitir lo que falta por catalogar la dejaría
   * coherente en euros y muda sobre la mitad de la cena. El artículo apunta al alimento por su id
   * —la referencia es blanda, sin clave foránea— y la pantalla ya sabe enseñar un id que no
   * resuelve.
   *
   * <p>Vacío cuando no hay plan activo, y entonces quien llama se queda con el catálogo entero como
   * hasta ahora.
   */
  private List<ShoppingListItem> itemsFromActivePlan(UUID userId) {
    Map<String, Double> gramsByFood = new LinkedHashMap<>();
    for (ResolvedDay day : plannedWeek.activePlanDays(userId)) {
      for (ResolvedMeal meal : day.meals()) {
        for (ResolvedItem item : meal.items()) {
          if (item.foodId() != null) {
            gramsByFood.merge(item.foodId(), item.grams(), Double::sum);
          }
        }
      }
    }
    if (gramsByFood.isEmpty()) {
      return List.of();
    }

    Map<String, CatalogStoreProduct> catalogByFood = new LinkedHashMap<>();
    for (CatalogStoreProduct product : storeProductRepository.findAll(null)) {
      if (product.foodId() != null) {
        // El primero que cubra ese alimento. Hay una cadena sola hoy; cuando haya más, esta es la
        // línea que leerá en cuál compra cada cual.
        catalogByFood.putIfAbsent(product.foodId(), product);
      }
    }
    Map<String, String> ownIdByStoreProduct =
        productRepository.findAllByOwner(userId).stream()
            .filter(stored -> stored.product().storeProductId() != null)
            .collect(
                Collectors.toMap(
                    stored -> stored.product().storeProductId(),
                    StoredShoppingProduct::id,
                    (first, second) -> first));

    List<ShoppingListItem> items = new ArrayList<>();
    gramsByFood.forEach(
        (foodId, grams) -> {
          CatalogStoreProduct product = catalogByFood.get(foodId);
          if (product == null) {
            // Sin precio: nadie lo ha dicho, y un cero diría que sale gratis.
            items.add(new ShoppingListItem(foodId, 1, null, false, ShoppingUnit.UD, null));
            return;
          }
          int packages =
              GroceryQuantityCalculator.packagesFor(
                  grams, product.packageAmount(), product.packageUnit());
          BigDecimal price = product.priceEur();
          items.add(
              new ShoppingListItem(
                  ownIdByStoreProduct.getOrDefault(product.id(), product.id()),
                  packages,
                  price == null ? null : price.multiply(BigDecimal.valueOf(packages)),
                  false,
                  ShoppingUnit.UD,
                  null));
        });
    return items;
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
    UUID userId = currentUserProvider.currentUserId();
    StoredShoppingListItem existing =
        listRepository
            .findItem(userId, itemId)
            .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));

    StoredShoppingProduct product = productsById(userId).get(existing.item().productId());
    if (product == null) {
      throw new NotFoundException(
          "El producto del artículo ya no existe: " + existing.item().productId());
    }

    BigDecimal newCost =
        product.product().estimatedPriceEur() == null
            ? null
            : product
                .product()
                .estimatedPriceEur()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

    return listRepository
        .updateQuantity(userId, itemId, quantity, newCost)
        .orElseThrow(() -> new NotFoundException("No existe el artículo: " + itemId));
  }

  private Map<String, StoredShoppingProduct> productsById(UUID userId) {
    return productRepository.findAllByOwner(userId).stream()
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
            : product.product().estimatedPriceEur() == null
                ? null
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
        product != null,
        estimatedCostEur,
        stored.item().checked(),
        stored.item().unit(),
        servings,
        productUrl);
  }
}

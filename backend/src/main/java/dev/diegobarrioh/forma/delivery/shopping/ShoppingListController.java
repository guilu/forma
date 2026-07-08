package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.application.ShoppingListService;
import dev.diegobarrioh.forma.application.StoredShoppingListItem;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weekly shopping list endpoints (FOR-39): read the checklist + budget, and toggle an item's
 * checked state. Mounted under {@link ApiPaths#V1}{@code /shopping/list}.
 *
 * <p>Thin controller (ADR-001, ADR-005): maps to/from delivery DTOs and delegates to {@link
 * ShoppingListService}. Absent list / unknown item become {@code NOT_FOUND} (404) via the FOR-27
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/shopping/list")
public class ShoppingListController {

  private final ShoppingListService service;

  public ShoppingListController(ShoppingListService service) {
    this.service = service;
  }

  /** Returns the current week's checklist with resolved names and budget. */
  @GetMapping
  public ShoppingListResponse currentList() {
    return ShoppingListResponse.from(service.currentView());
  }

  /** Sets an item's checked state. */
  @PatchMapping("/items/{id}/checked")
  public CheckedResponse setChecked(
      @PathVariable String id, @Valid @RequestBody SetCheckedRequest request) {
    StoredShoppingListItem updated = service.setChecked(id, request.checked());
    return new CheckedResponse(updated.id(), updated.item().checked());
  }

  /** Minimal response confirming an item's new checked state. */
  public record CheckedResponse(String id, boolean checked) {}
}

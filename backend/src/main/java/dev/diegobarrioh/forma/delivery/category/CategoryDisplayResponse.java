package dev.diegobarrioh.forma.delivery.category;

import dev.diegobarrioh.forma.application.CategoryDisplay;

/**
 * Response body for the category display endpoints (FOR-197).
 *
 * <p>Delivery read model, distinct from the application type (ADR-005). The scope travels as its
 * name so a client can group by it without guessing from the code.
 */
public record CategoryDisplayResponse(String scope, String code, String label, String icon) {

  public static CategoryDisplayResponse from(CategoryDisplay display) {
    return new CategoryDisplayResponse(
        display.scope().name(), display.code(), display.label(), display.icon());
  }
}

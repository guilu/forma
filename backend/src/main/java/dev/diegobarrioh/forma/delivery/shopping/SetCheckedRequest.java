package dev.diegobarrioh.forma.delivery.shopping;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/v1/shopping/list/items/{id}/checked} (FOR-39).
 *
 * @param checked required new checked state
 */
public record SetCheckedRequest(@NotNull Boolean checked) {}

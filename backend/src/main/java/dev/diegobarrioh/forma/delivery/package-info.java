/**
 * Delivery layer: REST controllers and API DTOs.
 *
 * <p>Orchestrates interaction but owns no business rules. Controllers stay thin and never expose
 * persistence entities. The versioned API base path ({@link
 * dev.diegobarrioh.forma.delivery.ApiPaths#V1}) and the consistent error response baseline ({@link
 * dev.diegobarrioh.forma.delivery.error.ApiError}, {@link
 * dev.diegobarrioh.forma.delivery.error.GlobalExceptionHandler}) are established by FOR-88. Product
 * endpoints are added by their own stories. See ADR-001 and ADR-005.
 */
package dev.diegobarrioh.forma.delivery;

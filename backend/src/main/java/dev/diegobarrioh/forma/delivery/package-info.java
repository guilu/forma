/**
 * Delivery layer: REST controllers and API DTOs.
 *
 * <p>Orchestrates interaction but owns no business rules. Controllers stay thin and never expose
 * persistence entities. The versioned product API and error baseline are owned by FOR-88; the
 * skeleton only relies on the Actuator health endpoint for startup verification. See ADR-001 and
 * ADR-005.
 */
package dev.diegobarrioh.forma.delivery;

package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Goal;

/**
 * A persisted {@link Goal} together with its generated id (FOR-125). The domain type carries no
 * identity (matching {@code StoredShoppingProduct}, FOR-36); persistence assigns the id.
 *
 * @param id the stored goal's id
 * @param goal the goal data, including its milestones (each with its own persisted id)
 */
public record StoredGoal(String id, Goal goal) {}

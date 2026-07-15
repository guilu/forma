package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealLogEntry;

/**
 * A persisted {@link MealLogEntry} together with its generated id (FOR-127), mirroring {@code
 * StoredGoal} (FOR-125).
 */
public record StoredMealLogEntry(String id, MealLogEntry entry) {}

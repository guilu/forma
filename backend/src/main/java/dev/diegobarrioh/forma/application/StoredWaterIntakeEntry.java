package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WaterIntakeEntry;

/**
 * A persisted {@link WaterIntakeEntry} together with its generated id (FOR-130), mirroring {@code
 * StoredMealLogEntry} (FOR-127).
 */
public record StoredWaterIntakeEntry(String id, WaterIntakeEntry entry) {}

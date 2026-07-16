package dev.diegobarrioh.forma.application;

import java.time.LocalDate;

/**
 * Input to {@link HydrationService#log}: the day the water was consumed and the logged volume (spec
 * FOR-130 api.md). Validated by {@link HydrationService}.
 *
 * @param date the day the water was consumed
 * @param volumeMl the logged volume in milliliters; must be strictly positive
 */
public record LogWaterIntakeCommand(LocalDate date, Double volumeMl) {}

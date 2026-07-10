package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Instant;
import java.util.List;

/**
 * The assembled weekly insights (FOR-45): the FOR-40 {@link WeeklyCheckIn} snapshot, the
 * prioritized {@code main} {@link Recommendation}, the remaining {@code secondary} ones, and the
 * {@code generatedAt} timestamp. Application read model produced on demand by {@link
 * WeeklyInsightsService}; a delivery DTO (ADR-005) maps it for the API.
 *
 * @param checkIn the week's snapshot (body + training)
 * @param main the highest-priority recommendation (never null — at least the body/training rules
 *     always emit one)
 * @param secondary the remaining recommendations in priority order; empty when there is only one
 * @param generatedAt when these insights were computed
 */
public record WeeklyInsights(
    WeeklyCheckIn checkIn,
    Recommendation main,
    List<Recommendation> secondary,
    Instant generatedAt) {}

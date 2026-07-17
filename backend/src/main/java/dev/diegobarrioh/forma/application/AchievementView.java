package dev.diegobarrioh.forma.application;

import java.time.Instant;

/**
 * One achievement as returned by {@link AchievementService#evaluate()} (FOR-135): catalog display
 * copy plus, only when earned, the moment it was earned. Application-level read model, distinct
 * from both the domain {@code Achievement} catalog entry and the delivery DTO (ADR-005), mirroring
 * the {@code HydrationProgress}/{@code DayConsumption} from-view convention.
 *
 * @param id the catalog achievement's stable id
 * @param title short display title
 * @param description short display description
 * @param earnedAt when this achievement was earned, or {@code null} when it is still available (not
 *     yet earned) — never fabricated
 */
public record AchievementView(String id, String title, String description, Instant earnedAt) {}

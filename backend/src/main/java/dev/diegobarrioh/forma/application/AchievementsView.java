package dev.diegobarrioh.forma.application;

import java.util.List;

/**
 * The full response of {@link AchievementService#evaluate()} (FOR-135): earned achievements (with
 * {@code earnedAt}) separated from the still-available ones. Never distinguishes "no data yet" from
 * an error — an owner with nothing earned still gets the full {@link #available()} catalog, never a
 * 404 (spec FOR-135 api.md).
 *
 * @param earned achievements the owner has earned, each with a non-null {@code earnedAt}
 * @param available catalog achievements not yet earned, each with a {@code null earnedAt}
 */
public record AchievementsView(List<AchievementView> earned, List<AchievementView> available) {}

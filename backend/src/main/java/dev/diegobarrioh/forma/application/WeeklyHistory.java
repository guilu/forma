package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import java.util.List;

/**
 * Weekly-history read model (FOR-139, slice 3 of FOR-104): an ordered, bounded per-week series
 * feeding the FOR-53 weekly-history bars. Application-level view, not persisted — computed fresh on
 * every read by {@link WeeklyHistoryService}, mirroring {@link Adherence}'s pattern.
 *
 * @param weeks ordered oldest-first, one bucket per week in the requested window; a week with no
 *     activity is still present as a zero bucket (spec FOR-139: "never omitted-as-error")
 */
public record WeeklyHistory(List<WeeklyHistoryBucket> weeks) {}

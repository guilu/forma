package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A per-day hydration aggregate (FOR-130, hydration slice of FOR-102): the water-intake entries
 * logged for one day, plus their accumulated total volume and progress toward a daily goal.
 *
 * <p>Derived-on-read by design (spec FOR-130 Open Questions), mirroring {@link MealLog} (FOR-127):
 * {@link #totalMl()} is always a fresh sum over {@link #entries}, never a separately
 * stored/maintained value, so the total can never drift out of sync with the entries. Aggregate
 * shape is per-entry rows summed in memory (same as {@link MealLog}) rather than a persisted
 * per-day summary row — the simplest choice consistent with the FOR-127 persistence style (spec
 * FOR-130 Open Questions).
 *
 * @param date the day this aggregate covers
 * @param entries the day's logged entries, in the order they were logged
 */
public record HydrationLog(LocalDate date, List<WaterIntakeEntry> entries) {

  public HydrationLog {
    Objects.requireNonNull(date, "date must not be null");
    entries = List.copyOf(entries);
  }

  /** An empty aggregate for {@code date} — the "no entries yet" case, never an error. */
  public static HydrationLog empty(LocalDate date) {
    return new HydrationLog(date, List.of());
  }

  /** Returns a new aggregate with {@code entry} appended (append-only, spec FOR-130). */
  public HydrationLog withEntry(WaterIntakeEntry entry) {
    Objects.requireNonNull(entry, "entry must not be null");
    List<WaterIntakeEntry> merged = new ArrayList<>(entries);
    merged.add(entry);
    return new HydrationLog(date, merged);
  }

  /** The day's total logged volume (ml): a fresh sum over every entry's volume. */
  public double totalMl() {
    double total = 0;
    for (WaterIntakeEntry entry : entries) {
      total += entry.volumeMl();
    }
    return total;
  }

  /**
   * Progress toward {@code goalMl}: {@code totalMl() / goalMl}. Returns {@code null} when the goal
   * cannot be determined ({@code goalMl} is {@code null} or not strictly positive) — spec FOR-130:
   * "When the goal cannot be determined, progress is null (not fabricated)."
   *
   * <p><b>Uncapped/raw</b> (documented decision, spec FOR-130 api.md: "Cap at 1.0 or report raw —
   * document"): a total exceeding the goal yields a value greater than {@code 1.0} rather than
   * being clamped. Capping would silently discard the information that the user drank more than
   * their goal, which the FOR-54 UI may want to surface (e.g. "150% of your goal").
   */
  public Double progressToward(Double goalMl) {
    if (goalMl == null || goalMl <= 0) {
      return null;
    }
    return totalMl() / goalMl;
  }
}

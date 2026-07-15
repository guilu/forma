package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A per-day meal-log aggregate (FOR-127): the entries logged for one day, plus their accumulated
 * consumed macro totals.
 *
 * <p>Derived-on-read by design (spec FOR-127 Open Questions): {@link #consumedTotals()} is always a
 * fresh sum over {@link #entries}, never a separately stored/maintained value, so consumed totals
 * can never drift out of sync with the entries they are built from. Aggregate shape is per-entry
 * rows summed in memory (mirrors the FOR-125 {@code Goal}/{@code Milestone} list-of-rows style)
 * rather than a persisted per-day summary row.
 *
 * @param date the day this aggregate covers
 * @param entries the day's logged entries, in the order they were logged
 */
public record MealLog(LocalDate date, List<MealLogEntry> entries) {

  public MealLog {
    Objects.requireNonNull(date, "date must not be null");
    entries = List.copyOf(entries);
  }

  /** An empty aggregate for {@code date} — the "no logs yet" case, never an error. */
  public static MealLog empty(LocalDate date) {
    return new MealLog(date, List.of());
  }

  /** Returns a new aggregate with {@code entry} appended (append-only, spec FOR-127). */
  public MealLog withEntry(MealLogEntry entry) {
    Objects.requireNonNull(entry, "entry must not be null");
    List<MealLogEntry> merged = new ArrayList<>(entries);
    merged.add(entry);
    return new MealLog(date, merged);
  }

  /** The day's consumed macro totals: a fresh sum over every entry's own totals. */
  public NutritionTotals consumedTotals() {
    int calories = 0;
    double protein = 0;
    double carbs = 0;
    double fat = 0;
    for (MealLogEntry entry : entries) {
      calories += entry.totals().calories();
      protein += entry.totals().proteinG();
      carbs += entry.totals().carbsG();
      fat += entry.totals().fatG();
    }
    return new NutritionTotals(calories, round1(protein), round1(carbs), round1(fat));
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}

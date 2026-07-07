package dev.diegobarrioh.forma.domain;

/**
 * Computed nutrition totals (FOR-32): calories and macro grams for a meal or a day.
 *
 * <p>Framework-free value produced by {@link NutritionCalculator}. Values are rounded for display
 * (calories to whole kcal, grams to one decimal) — the calculator sums the raw contributions first
 * and rounds once, so summing does not accumulate rounding error.
 *
 * @param calories total energy in kilocalories
 * @param proteinG total protein grams
 * @param carbsG total carbohydrate grams
 * @param fatG total fat grams
 */
public record NutritionTotals(int calories, double proteinG, double carbsG, double fatG) {}

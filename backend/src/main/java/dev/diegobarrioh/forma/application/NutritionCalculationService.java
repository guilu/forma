package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealTemplate;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing nutrition macro calculation (FOR-32).
 *
 * <p>Thin service over the pure {@link NutritionCalculator} domain calculation so later stories
 * (FOR-33 seed validation, and a future API/frontend) can compute meal/day totals and compare a day
 * to its targets. Mirrors the FOR-21/FOR-28 service pattern.
 */
@Service
public class NutritionCalculationService {

  /** Totals for a single meal. */
  public NutritionTotals mealTotals(MealTemplate meal) {
    return NutritionCalculator.mealTotals(meal);
  }

  /** Totals for a full day (sum of its meals). */
  public NutritionTotals dayTotals(List<MealTemplate> meals) {
    return NutritionCalculator.dayTotals(meals);
  }

  /** Whether a day's totals reach the given day template's targets, per macro. */
  public TargetComparison compareToTargets(List<MealTemplate> meals, NutritionDayTemplate target) {
    return TargetComparison.of(dayTotals(meals), target);
  }
}

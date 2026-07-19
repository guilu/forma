package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Shopping cost rule (FOR-150 rule 6, epic FOR-148 "Personalizar FORMA a Diego", sheet *Reglas*):
 * turns the FOR-152 {@link ShoppingBudget#overThreshold()} signal into at most one {@code SHOPPING}
 * {@link Recommendation} suggesting a cheaper protein swap. Pure, framework-free domain logic
 * (ADR-001); the threshold comparison itself already lives in {@link ShoppingBudgetCalculator}
 * (FOR-152) — this rule only reads the resulting flag and produces the explainable message, per
 * spec FOR-150 ("consuming FOR-152's overThreshold, not reintroducing a threshold").
 */
public final class ShoppingCostRules {

  private ShoppingCostRules() {}

  /** Evaluates the weekly shopping budget, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(ShoppingBudget budget, Instant createdAt) {
    if (budget == null || !budget.overThreshold()) {
      return List.of();
    }
    return List.of(overThreshold(budget, createdAt));
  }

  private static Recommendation overThreshold(ShoppingBudget budget, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "La compra semanal (%.2f €) supera el límite de %.2f €.",
            budget.weeklyEur(),
            budget.weeklyThresholdEur());
    return new Recommendation(
        createdAt,
        RecommendationCategory.SHOPPING,
        RecommendationSeverity.ACTION,
        "Coste alto; cambia salmón por merluza/atún/huevos.",
        reason,
        "weeklyCostEur");
  }
}

package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ShoppingCostRules} evaluator (FOR-150 rule 6): reads the FOR-152 {@link
 * ShoppingBudget#overThreshold()} signal (&gt;120 €/week) and suggests swapping salmon for a
 * cheaper protein source.
 */
class ShoppingCostRulesTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

  private static ShoppingBudget budget(String weeklyEur, boolean overThreshold) {
    return new ShoppingBudget(
        new BigDecimal(weeklyEur),
        new BigDecimal(weeklyEur),
        new BigDecimal("120.00"),
        overThreshold);
  }

  @Test
  void overThresholdFiresWithTheSalmonSwapMessage() {
    List<Recommendation> recs = ShoppingCostRules.evaluate(budget("135.00", true), NOW);

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.SHOPPING);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.message()).contains("salmón");
    assertThat(rec.reason()).contains("120");
    assertThat(rec.relatedMetric()).isEqualTo("weeklyCostEur");
    assertThat(rec.createdAt()).isEqualTo(NOW);
  }

  @Test
  void exactlyAtThresholdDoesNotFire() {
    // ShoppingBudgetCalculator already treats exactly 120.00 as not over threshold (Excel ">120").
    List<Recommendation> recs = ShoppingCostRules.evaluate(budget("120.00", false), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void underThresholdDoesNotFire() {
    List<Recommendation> recs = ShoppingCostRules.evaluate(budget("90.00", false), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void nullBudgetDoesNotFire() {
    assertThat(ShoppingCostRules.evaluate(null, NOW)).isEmpty();
  }
}

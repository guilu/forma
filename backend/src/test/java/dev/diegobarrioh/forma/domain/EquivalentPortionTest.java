package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * How much of one food replaces a portion of another (V47). Plain JUnit 5 + AssertJ (ADR-007).
 *
 * <p>The weight is the RESULT of an equivalence, never its criterion: what a curator states is
 * which nutrient is being matched and how big a portion to talk about, and the grams follow from
 * the catalog. That is why this is a calculation and not a stored column — the answer has to move
 * when somebody corrects a food's macros, or it starts lying.
 */
class EquivalentPortionTest {

  private static final FoodItem RICE = new FoodItem("rice", "Arroz", 360, 7.0, 79.0, 1.0, 80);
  private static final FoodItem POTATO = new FoodItem("potato", "Patata", 77, 2.0, 17.0, 0.1, 300);
  private static final FoodItem CHICKEN =
      new FoodItem("chicken", "Pechuga pollo", 110, 23.0, 0.0, 2.0, 200);
  private static final FoodItem FISH = new FoodItem("fish", "Merluza", 74, 16.0, 0.0, 1.0, 200);
  private static final FoodItem OIL = new FoodItem("olive-oil", "Aceite", 900, 0.0, 0.0, 100.0, 10);

  @Test
  void matchesTheNutrientTheCuratorChose() {
    // 100 g of rice carries 79 g of carbohydrate; potato carries 17 g per 100 g.
    EquivalentPortion portion = EquivalentPortion.of(RICE, POTATO, EquivalenceBasis.CARBS, 100.0);

    assertThat(portion.targetReferenceG()).isCloseTo(464.7, within(0.1));
  }

  /** Protein for protein, which is what a swap between two lean animal products is about. */
  @Test
  void matchesProteinBetweenTwoProteins() {
    EquivalentPortion portion =
        EquivalentPortion.of(CHICKEN, FISH, EquivalenceBasis.PROTEIN, 200.0);

    // 200 g chicken = 46 g protein; fish carries 16 g per 100 g -> 287.5 g.
    assertThat(portion.targetReferenceG()).isCloseTo(287.5, within(0.1));
  }

  /**
   * The nutrient being matched deviates by nothing, by construction, so reporting a number for it
   * would be noise. The others are exactly what somebody needs to judge the swap.
   */
  @Test
  void reportsHowFarTheOtherMacrosDrift() {
    EquivalentPortion portion = EquivalentPortion.of(RICE, POTATO, EquivalenceBasis.CARBS, 100.0);

    assertThat(portion.carbsDeviationPct()).isNull();
    assertThat(portion.proteinDeviationPct()).isCloseTo(32.8, within(0.1));
    assertThat(portion.fatDeviationPct()).isCloseTo(-53.5, within(0.1));
    assertThat(portion.caloriesDeviationPct()).isCloseTo(-0.6, within(0.1));
  }

  /** Swapping a food for itself is not a swap; the deviations would all be zero and say nothing. */
  @Test
  void refusesAFoodStandingInForItself() {
    assertThatThrownBy(() -> EquivalentPortion.of(RICE, RICE, EquivalenceBasis.CARBS, 100.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * A target with none of the nutrient cannot supply it at any weight — the arithmetic divides by
   * zero, and no amount of hake makes up the carbohydrate in rice.
   */
  @Test
  void refusesATargetWithNoneOfTheNutrient() {
    assertThatThrownBy(() -> EquivalentPortion.of(RICE, CHICKEN, EquivalenceBasis.CARBS, 100.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("CARBS");
  }

  /**
   * And a source with none of it asks for nothing: "0 g of rice replaces 100 g of olive oil" is
   * arithmetically fine and nutritionally meaningless, so it is refused too.
   */
  @Test
  void refusesASourceWithNoneOfTheNutrient() {
    assertThatThrownBy(() -> EquivalentPortion.of(OIL, RICE, EquivalenceBasis.PROTEIN, 10.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("PROTEIN");
  }

  @Test
  void refusesAPortionThatIsNotAPortion() {
    assertThatThrownBy(() -> EquivalentPortion.of(RICE, POTATO, EquivalenceBasis.CARBS, 0.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * The tolerance never blocks anything — the swap was asked for on carbohydrate and it delivers
   * carbohydrate. It only says whether the collateral drift is worth mentioning.
   */
  @Test
  void tellsWhetherTheDriftPassesAToleranceWithoutRefusingAnything() {
    EquivalentPortion portion = EquivalentPortion.of(RICE, POTATO, EquivalenceBasis.CARBS, 100.0);

    assertThat(portion.exceeds(25.0)).isTrue(); // fat drifts 53.5%
    assertThat(portion.exceeds(60.0)).isFalse();
    // No tolerance stated is not a breach; it is nobody having said what "too far" means.
    assertThat(portion.exceeds(null)).isFalse();
  }

  /**
   * A macro that is zero on both sides has not drifted; a percentage of nothing is not a number.
   */
  @Test
  void reportsNoDriftForAMacroThatIsZeroOnBothSides() {
    EquivalentPortion portion =
        EquivalentPortion.of(CHICKEN, FISH, EquivalenceBasis.PROTEIN, 200.0);

    assertThat(portion.carbsDeviationPct()).isNull();
    assertThat(portion.exceeds(1.0)).isTrue(); // fat and calories still drift
  }
}

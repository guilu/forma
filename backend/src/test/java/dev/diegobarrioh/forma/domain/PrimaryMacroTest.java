package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link PrimaryMacro} (V44): which macro a food is mostly made of, by
 * calories rather than by grams. Plain JUnit 5 + AssertJ (ADR-007).
 */
class PrimaryMacroTest {

  @Test
  void picksTheMacroThatContributesMostCalories() {
    // Oats: 13 g protein (52 kcal), 60 g carbs (240 kcal), 7 g fat (63 kcal).
    assertThat(PrimaryMacro.dominantOf(13.0, 60.0, 7.0)).contains(PrimaryMacro.CARBS);
    // Chicken breast: 23 g protein (92 kcal), 0 g carbs, 2 g fat (18 kcal).
    assertThat(PrimaryMacro.dominantOf(23.0, 0.0, 2.0)).contains(PrimaryMacro.PROTEIN);
    // Olive oil: fat and nothing else.
    assertThat(PrimaryMacro.dominantOf(0.0, 0.0, 100.0)).contains(PrimaryMacro.FAT);
  }

  /**
   * By calories, not by grams — the whole reason this is computed rather than eyeballed. A whole
   * egg carries more protein than fat by weight (13 g vs 10 g) and is still mostly fat on the plate
   * (52 kcal vs 90 kcal), because a gram of fat is worth more than twice a gram of protein.
   */
  @Test
  void weighsAGramOfFatMoreThanAGramOfProteinOrCarbohydrate() {
    assertThat(PrimaryMacro.dominantOf(13.0, 1.0, 10.0)).contains(PrimaryMacro.FAT);
  }

  /**
   * A food with no macros at all has no dominant one. Water and black coffee are real catalog
   * entries since the calorie floor dropped to zero, and answering "protein" for water would be
   * fabricating (FOR-134).
   */
  @Test
  void hasNoAnswerForAFoodWithNoMacros() {
    assertThat(PrimaryMacro.dominantOf(0.0, 0.0, 0.0)).isEmpty();
  }

  /**
   * A tie is not a winner. Two macros contributing the same calories means nothing decides it, and
   * picking the one that happens to be checked first would dress an arbitrary choice as a
   * measurement. Empty leaves the decision to whoever knows the food.
   */
  @Test
  void hasNoAnswerWhenTwoMacrosTie() {
    // 10 g protein and 10 g carbs are 40 kcal each.
    assertThat(PrimaryMacro.dominantOf(10.0, 10.0, 1.0)).isEmpty();
    // 9 g protein (36 kcal) and 4 g fat (36 kcal).
    assertThat(PrimaryMacro.dominantOf(9.0, 0.0, 4.0)).isEmpty();
  }

  /** Missing macros are not zero macros: nothing is known, so nothing is claimed. */
  @Test
  void hasNoAnswerWhenAMacroIsUnknown() {
    assertThat(PrimaryMacro.dominantOf(null, 60.0, 7.0)).isEmpty();
    assertThat(PrimaryMacro.dominantOf(13.0, null, 7.0)).isEmpty();
    assertThat(PrimaryMacro.dominantOf(13.0, 60.0, null)).isEmpty();
  }
}

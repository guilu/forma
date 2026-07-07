package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.MealTemplate;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link NutritionCalculationService} (FOR-32): delegates to the domain calculator
 * and compares a day to its targets (no Spring context — ADR-007).
 */
class NutritionCalculationServiceTest {

  private final NutritionCalculationService service = new NutritionCalculationService();

  private static MealTemplate meal(List<MealItem> items) {
    return new MealTemplate(
        NutritionDayType.STRENGTH, MealType.LUNCH, "Comida", LocalTime.of(14, 0), items, null);
  }

  @Test
  void computesDayTotalsAndComparesToTargets() {
    List<MealTemplate> day = List.of(meal(List.of(new MealItem("chicken", 200))));

    assertThat(service.dayTotals(day).calories()).isEqualTo(330); // 165 * 2

    NutritionDayTemplate lowTarget =
        new NutritionDayTemplate(NutritionDayType.STRENGTH, 300, 50, 10, 5, null);
    // chicken 200 g = 330 kcal, 62 g protein — reaches these modest targets.
    assertThat(service.compareToTargets(day, lowTarget).caloriesReached()).isTrue();
    assertThat(service.compareToTargets(day, lowTarget).proteinReached()).isTrue();
  }
}

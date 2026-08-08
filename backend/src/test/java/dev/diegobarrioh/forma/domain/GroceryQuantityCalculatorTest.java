package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Cuántos envases cubren la semana. */
class GroceryQuantityCalculatorTest {

  /** El caso del enunciado: 500 g de avena bastan para una semana que pide 400. */
  @Test
  void onePackageWhenItCoversTheWeek() {
    assertThat(GroceryQuantityCalculator.packagesFor(400, new BigDecimal("500"), "G")).isEqualTo(1);
  }

  /** Y dos cuando no: 560 g no caben en una bolsa de 500, por poco que se pase. */
  @Test
  void roundsUpWhenOnePackageFallsShort() {
    assertThat(GroceryQuantityCalculator.packagesFor(560, new BigDecimal("500"), "G")).isEqualTo(2);
  }

  @Test
  void convertsKilosToGrams() {
    assertThat(GroceryQuantityCalculator.packagesFor(2500, new BigDecimal("1"), "KG")).isEqualTo(3);
  }

  /** Justo al borde no se compra de más: 1000 g son exactamente un kilo. */
  @Test
  void anExactFitIsOnePackage() {
    assertThat(GroceryQuantityCalculator.packagesFor(1000, new BigDecimal("1"), "KG")).isEqualTo(1);
  }

  /** Granel y envases sin peso declarado: una unidad, sin fingir un cálculo. */
  @Test
  void oneUnitWhenTheShopDoesNotSayHowMuchThePackageHolds() {
    assertThat(GroceryQuantityCalculator.packagesFor(700, null, null)).isEqualTo(1);
    assertThat(GroceryQuantityCalculator.packagesFor(700, new BigDecimal("1"), null)).isEqualTo(1);
  }

  /**
   * Litros y unidades no se convierten a gramos.
   *
   * <p>Un litro de aceite no pesa un kilo, y un huevo no pesa nada en particular: tratarlos como
   * masa sería un cálculo con aspecto de exacto y un error dentro.
   */
  @Test
  void doesNotTreatVolumeOrCountAsMass() {
    assertThat(GroceryQuantityCalculator.packagesFor(2000, new BigDecimal("1"), "L")).isEqualTo(1);
    assertThat(GroceryQuantityCalculator.packagesFor(2000, new BigDecimal("12"), "UD"))
        .isEqualTo(1);
  }

  /** Un alimento que la semana no pide es una unidad, no cero: nadie apunta «cero bolsas». */
  @Test
  void neverAsksForLessThanOne() {
    assertThat(GroceryQuantityCalculator.packagesFor(0, new BigDecimal("500"), "G")).isEqualTo(1);
  }
}

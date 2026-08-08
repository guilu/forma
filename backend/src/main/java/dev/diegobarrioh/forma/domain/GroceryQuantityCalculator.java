package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Cuántos envases hay que comprar para cubrir los gramos que pide el plan.
 *
 * <p>Puro y sin dependencias (ADR-001): entra lo que la semana necesita y lo que la tienda vende,
 * sale un número de envases. Aquí no se lee ni se escribe nada.
 *
 * <p><b>Se redondea siempre hacia arriba.</b> 560 g de avena en bolsas de 500 g son dos bolsas, no
 * una y pico: en la compra no existe la fracción de envase, y quedarse corto es peor que sobrar —
 * uno se arregla la semana que viene y el otro te deja sin desayunar el domingo.
 *
 * <p><b>Un envase sin cantidad declarada pide una unidad.</b> Los plátanos se venden «kg» y la
 * ensalada en «bolsa»: la tienda no dice cuánto trae, así que dividir exigiría inventarse el peso.
 * Una unidad es lo que alguien apuntaría a mano, y es honesto porque no finge un cálculo.
 */
public final class GroceryQuantityCalculator {

  private GroceryQuantityCalculator() {}

  /**
   * Envases necesarios para cubrir {@code neededGrams}.
   *
   * @param neededGrams lo que suma la semana del plan para ese alimento
   * @param packageAmount lo que trae el envase, o {@code null} si la tienda no lo dice
   * @param packageUnit la unidad de esa cantidad ("g", "kg", "l", "ud"), tal y como la declara la
   *     tienda
   * @return al menos 1, siempre
   */
  public static int packagesFor(double neededGrams, BigDecimal packageAmount, String packageUnit) {
    Double packageGrams = gramsPerPackage(packageAmount, packageUnit);
    if (packageGrams == null || packageGrams <= 0 || neededGrams <= 0) {
      return 1;
    }
    return (int) Math.max(1, Math.ceil(neededGrams / packageGrams));
  }

  /**
   * Los gramos que trae un envase, o {@code null} cuando no se puede saber.
   *
   * <p>Solo se convierten las unidades de masa. Un litro de leche pesa aproximadamente un kilo y un
   * litro de aceite no, así que tratar el volumen como masa metería un error de casi el 10% en el
   * aceite para ahorrarse una unidad — y las unidades sueltas («12 uds» de huevos) no se convierten
   * en gramos de ninguna manera sensata sin saber lo que pesa un huevo. En ambos casos la respuesta
   * honesta es que este cálculo no aplica.
   */
  private static Double gramsPerPackage(BigDecimal amount, String unit) {
    if (amount == null || unit == null) {
      return null;
    }
    return switch (unit.toUpperCase(Locale.ROOT)) {
      case "G" -> amount.doubleValue();
      case "KG" -> amount.doubleValue() * 1000;
      default -> null;
    };
  }
}

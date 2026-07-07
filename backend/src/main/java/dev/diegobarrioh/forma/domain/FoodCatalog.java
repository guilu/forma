package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The initial food catalog (FOR-30): the common foods used by the nutrition plan, with estimated
 * per-100 g nutrition values.
 *
 * <p>Defined in code with stable ids rather than as persisted seed data — no nutrition persistence
 * exists yet and meal items (FOR-31) only need to reference foods by a stable id (spec FOR-30 Open
 * Questions, consistent with the FOR-24 exercise catalog). No migration is introduced. Values are
 * sensible MVP estimates per 100 g, not medical data; product/price data lives in Shopping (FOR-5).
 */
public final class FoodCatalog {

  private static final List<FoodItem> FOODS =
      List.of(
          new FoodItem("oats", "Avena", 389, 16.9, 66.3, 6.9, 60),
          new FoodItem("eggs", "Huevos", 155, 13.0, 1.1, 11.0, 100),
          new FoodItem("yogurt", "Yogur natural", 61, 3.5, 4.7, 3.3, 125),
          new FoodItem("fresh-cheese", "Queso fresco", 98, 11.0, 3.4, 4.0, 100),
          new FoodItem("chicken", "Pollo (pechuga)", 165, 31.0, 0.0, 3.6, 150),
          new FoodItem("turkey", "Pavo (pechuga)", 135, 29.0, 0.0, 1.0, 150),
          new FoodItem("fish", "Pescado blanco", 96, 20.0, 0.0, 1.5, 150),
          new FoodItem("rice", "Arroz cocido", 130, 2.7, 28.0, 0.3, 150),
          new FoodItem("potato", "Patata cocida", 87, 2.0, 20.0, 0.1, 200),
          new FoodItem("banana", "Plátano", 89, 1.1, 22.8, 0.3, 120),
          new FoodItem("vegetables", "Verduras mixtas", 40, 2.5, 7.0, 0.4, 200),
          new FoodItem("whey-protein", "Proteína whey", 400, 80.0, 8.0, 6.0, 30));

  private FoodCatalog() {}

  /** All catalog foods (immutable). */
  public static List<FoodItem> foods() {
    return FOODS;
  }

  /** Finds a food by its stable id. */
  public static Optional<FoodItem> findById(String id) {
    return FOODS.stream().filter(food -> food.id().equals(id)).findFirst();
  }
}

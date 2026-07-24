package dev.diegobarrioh.forma.application;

import java.math.BigDecimal;

/**
 * Read model for a persisted {@code food_catalog} row (FOR-173, ADR-011).
 *
 * <p>Deliberately separate from {@link dev.diegobarrioh.forma.domain.FoodItem}: {@code FoodItem}'s
 * compact constructor enforces {@code kcalPer100g > 0} and a required, non-nullable primitive
 * {@code defaultServingG > 0}. The persisted read model must faithfully carry a nullable {@code
 * servingSizeG} (ADR-011 allows null) without coupling persistence to the static nutrition domain's
 * validation/naming, mirroring FOR-172's {@code CatalogExercise} decision.
 *
 * @param id stable catalog id, verbatim from {@code domain.FoodCatalog}
 * @param name human-readable food name
 * @param servingSizeG a sensible default serving in grams; nullable
 * @param kcal energy per 100 g in kilocalories; not null
 * @param proteinG protein grams per 100 g; not null
 * @param carbsG carbohydrate grams per 100 g; not null
 * @param fatG fat grams per 100 g; not null
 * @param fiberG fibre grams per 100 g, or {@code null} if unknown (FOR-134, never fabricated)
 * @param sugarsG sugars grams per 100 g, or {@code null} if unknown
 * @param sodiumMg sodium milligrams per 100 g, or {@code null} if unknown
 * @param saturatedFatG saturated fat grams per 100 g, or {@code null} if unknown
 */
public record CatalogFood(
    String id,
    String name,
    BigDecimal servingSizeG,
    int kcal,
    BigDecimal proteinG,
    BigDecimal carbsG,
    BigDecimal fatG,
    BigDecimal fiberG,
    BigDecimal sugarsG,
    BigDecimal sodiumMg,
    BigDecimal saturatedFatG) {}

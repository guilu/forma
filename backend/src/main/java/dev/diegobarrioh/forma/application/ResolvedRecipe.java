package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.util.List;

/**
 * A recipe together with what it works out to nutritionally (V52).
 *
 * <p>Two halves stored differently on purpose: {@link #recipe} is what somebody wrote down, {@link
 * #total} and {@link #perServing} are arithmetic over the current catalog. Handing them over
 * separately would make every caller redo the sum, and eventually one of them would cache it.
 *
 * @param total the whole dish
 * @param perServing the same divided by {@code servings} — the figure anybody eating it actually
 *     wants, and the one that is wrong fourfold if a stew for four is read as a meal for one
 * @param unknownFoodIds ingredients whose food is no longer in the catalog. Should be empty, since
 *     a foreign key protects them; carried rather than thrown so a dish with one bad line still
 *     renders the rest instead of disappearing
 */
public record ResolvedRecipe(
    Recipe recipe,
    NutritionTotals total,
    NutritionTotals perServing,
    List<String> unknownFoodIds) {}

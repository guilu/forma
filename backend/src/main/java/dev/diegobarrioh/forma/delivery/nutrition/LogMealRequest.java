package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.LogMealCommand;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/nutrition/log} (FOR-127 api.md, key nutrients added by
 * FOR-134 api.md).
 *
 * <p>Delivery DTO, distinct from the {@link dev.diegobarrioh.forma.domain.MealLogEntry} domain type
 * (ADR-005). {@code mealType} is validated as a {@code String} against the known {@link MealType}
 * names here (not the enum type itself) so an unknown value yields {@code VALIDATION_ERROR} instead
 * of a Jackson enum-parse failure surfacing as 500, mirroring {@code CreateGoalRequest.metric}
 * (FOR-125). Cross-field validation (exactly one of {@code foodItemId}+{@code portions} or free
 * macros) is genuinely business logic — it is done by {@link
 * dev.diegobarrioh.forma.application.MealLogService}, not here, so it stays testable without a web
 * context and consistent with the "unknown foodItemId" check which also needs the FOR-30 catalog.
 *
 * <p><b>Key nutrients (FOR-134).</b> {@code fiberG}/{@code sugarsG}/{@code sodiumMg}/{@code
 * saturatedFatG} are optional and only meaningful for a free/ad-hoc entry — a catalog entry's key
 * nutrients are always derived from the resolved {@link dev.diegobarrioh.forma.domain.FoodItem}, so
 * these fields are ignored when {@code foodItemId} is provided. Validated non-negative when
 * present, same {@code @PositiveOrZero} pattern as the existing macro fields.
 *
 * @param date required, ISO-8601
 * @param mealType required; one of the {@link MealType} names, read from the enum rather than
 *     re-listed here
 * @param plannedMealId which planned meal this answers (V55); optional, and null for the ordinary
 *     unplanned entry
 * @param foodItemId FOR-30 catalog food id; provide with {@code portions} for a catalog entry
 * @param portions how many portions; of {@code servingId} when given, of the food's default one
 *     otherwise. Must be positive when present
 * @param grams the amount in grams; must be positive when present. The way to log a food that has
 *     no portion recorded at all, which used to be impossible
 * @param servingId a named portion of that food (V49) that {@code portions} counts
 * @param name free entry's name; provide with the macro fields for a free/ad-hoc entry
 * @param kcal free entry's calories; must be non-negative when present
 * @param proteinG free entry's protein grams; must be non-negative when present
 * @param carbsG free entry's carbohydrate grams; must be non-negative when present
 * @param fatG free entry's fat grams; must be non-negative when present
 * @param fiberG free entry's optional fibre grams (FOR-134); must be non-negative when present
 * @param sugarsG free entry's optional sugars grams (FOR-134); must be non-negative when present
 * @param sodiumMg free entry's optional sodium milligrams (FOR-134); must be non-negative when
 *     present
 * @param saturatedFatG free entry's optional saturated fat grams (FOR-134); must be non-negative
 *     when present
 */
public record LogMealRequest(
    @NotNull LocalDate date,
    @NotBlank String mealType,
    String foodItemId,
    @Positive Double portions,
    @Positive Double grams,
    String servingId,
    String name,
    @PositiveOrZero Integer kcal,
    @PositiveOrZero Double proteinG,
    @PositiveOrZero Double carbsG,
    @PositiveOrZero Double fatG,
    @PositiveOrZero Double fiberG,
    @PositiveOrZero Double sugarsG,
    @PositiveOrZero Integer sodiumMg,
    @PositiveOrZero Double saturatedFatG,
    java.util.UUID plannedMealId) {

  /** Builds the application-layer command; cross-field validation happens in the service. */
  public LogMealCommand toCommand() {
    return new LogMealCommand(
        date,
        mealTypeOrFail(),
        foodItemId,
        portions,
        grams,
        servingId,
        name,
        kcal,
        proteinG,
        carbsG,
        fatG,
        fiberG,
        sugarsG,
        sodiumMg,
        saturatedFatG,
        plannedMealId);
  }

  /**
   * The meal type, read from the enum itself.
   *
   * <p>This used to be an {@code @Pattern} listing the six names, and that list went stale the day
   * V53 added SNACK: the merienda existed in the domain, in the database and in the plan editor,
   * and could not be logged. A regular expression naming a closed enum's values is the same fact in
   * two places, and the copy has no way of knowing when the original changes. Reading the enum
   * cannot go stale.
   *
   * @throws ValidationException when the name is not one of {@link MealType}'s — a 400, not the 500
   *     a bare {@code valueOf} would raise
   */
  private MealType mealTypeOrFail() {
    try {
      return MealType.valueOf(mealType.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw new ValidationException("Momento del día desconocido: " + mealType);
    }
  }
}

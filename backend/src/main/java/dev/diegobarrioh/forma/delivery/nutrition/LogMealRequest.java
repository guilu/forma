package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.LogMealCommand;
import dev.diegobarrioh.forma.domain.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
 * @param mealType required; one of the {@link MealType} names
 * @param foodItemId FOR-30 catalog food id; provide with {@code portions} for a catalog entry
 * @param portions number of the food's default servings; must be positive when present
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
    @NotBlank
        @Pattern(
            regexp = "BREAKFAST|MID_MORNING|LUNCH|PRE_WORKOUT|POST_WORKOUT|DINNER",
            message =
                "must be one of BREAKFAST, MID_MORNING, LUNCH, PRE_WORKOUT, POST_WORKOUT, DINNER")
        String mealType,
    String foodItemId,
    @Positive Double portions,
    String name,
    @PositiveOrZero Integer kcal,
    @PositiveOrZero Double proteinG,
    @PositiveOrZero Double carbsG,
    @PositiveOrZero Double fatG,
    @PositiveOrZero Double fiberG,
    @PositiveOrZero Double sugarsG,
    @PositiveOrZero Integer sodiumMg,
    @PositiveOrZero Double saturatedFatG) {

  /** Builds the application-layer command; cross-field validation happens in the service. */
  public LogMealCommand toCommand() {
    return new LogMealCommand(
        date,
        MealType.valueOf(mealType),
        foodItemId,
        portions,
        name,
        kcal,
        proteinG,
        carbsG,
        fatG,
        fiberG,
        sugarsG,
        sodiumMg,
        saturatedFatG);
  }
}

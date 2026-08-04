package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealLog;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionDayTypeResolver;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Application use cases for meal consumption logging and the day consumption read model
 * (FOR-127/FOR-128, first implementable slices of FOR-102). Macros (kcal/protein/carbs/fat) plus,
 * since FOR-134, consumed key nutrients (fibra/azúcares/sodio/grasas saturadas); hydration is a
 * separate FOR-102 slice.
 *
 * <p>Real multi-user auth (FOR-145b-1, ADR-012): every use case resolves the caller's account id
 * via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID = "default-user"}
 * constant (removed by this slice). Never logs entry contents (personal health data) — see method
 * javadoc.
 *
 * <p><b>Plan-target resolution (FOR-128, moved onto real plans by V53/V54).</b> {@link
 * #consumption} resolves {@code date} to a {@link NutritionDayType} via {@link
 * NutritionDayTypeResolver} (which itself reuses the shared training day-classification — no
 * duplicated policy, no circular dependency on any training service), asks {@link DayTargetSource}
 * what the caller's ACTIVE PLAN targets for that kind of day, and compares it to the day's consumed
 * totals via {@link TargetComparison#of}.
 *
 * <p>That target used to come from three constants compiled into the jar, identical for every
 * account. It now comes from a plan somebody owns and can edit, which makes {@code target}/{@code
 * comparison} genuinely {@code null} when there is no active plan — an ordinary state for a new
 * account, not the fail-safe it was.
 *
 * <p><b>Key nutrients (FOR-134).</b> A catalog entry's key nutrients come from the resolved {@link
 * FoodItem}; a free entry's are optional caller input, validated non-negative when present. {@link
 * #consumption} sums them via {@code MealLog#consumedKeyNutrients()} under the documented
 * null/partial rule (a nutrient's day total is {@code null} if any logged entry lacks it). See
 * {@link MealLogEntry}'s javadoc for a known persistence limitation.
 */
@Service
public class MealLogService {

  private final MealLogRepository repository;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;
  private final FoodCatalogService foods;
  private final DayTargetSource planTargets;

  public MealLogService(
      MealLogRepository repository,
      Clock clock,
      CurrentUserProvider currentUserProvider,
      FoodCatalogService foods,
      DayTargetSource planTargets) {
    this.repository = repository;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
    this.foods = foods;
    this.planTargets = planTargets;
  }

  /**
   * Logs a consumed meal entry for the owner. Never logs {@code command} contents (personal health
   * data, AGENTS.md) — only the outcome (entry id) may be logged by callers if needed.
   *
   * @throws ValidationException if the date is missing/far in the future, {@code mealType} is
   *     missing, neither a catalog food nor free macros are provided (or both are), {@code
   *     foodItemId} is unknown, or {@code portions}/macros are invalid
   */
  public StoredMealLogEntry log(LogMealCommand command) {
    validateDate(command.date());
    if (command.mealType() == null) {
      throw new ValidationException("mealType is required");
    }

    boolean hasCatalogRef = command.foodItemId() != null;
    boolean hasFreeMacros =
        command.name() != null
            || command.kcal() != null
            || command.proteinG() != null
            || command.carbsG() != null
            || command.fatG() != null;

    MealLogEntry entry;
    if (hasCatalogRef && hasFreeMacros) {
      throw new ValidationException(
          "Provide either foodItemId+portions or free-item macros, not both");
    } else if (hasCatalogRef) {
      if (command.portions() == null || command.portions() <= 0) {
        throw new ValidationException("portions must be strictly positive");
      }
      FoodItem food =
          foods
              .findById(command.foodItemId())
              .orElseThrow(
                  () -> new ValidationException("unknown foodItemId: " + command.foodItemId()));
      entry =
          MealLogEntry.fromCatalog(command.date(), command.mealType(), food, command.portions());
    } else if (hasFreeMacros) {
      if (command.name() == null || command.name().isBlank()) {
        throw new ValidationException("name is required for a free/ad-hoc entry");
      }
      if (command.kcal() == null
          || command.proteinG() == null
          || command.carbsG() == null
          || command.fatG() == null) {
        throw new ValidationException("kcal, proteinG, carbsG and fatG are required");
      }
      if (command.kcal() < 0
          || command.proteinG() < 0
          || command.carbsG() < 0
          || command.fatG() < 0) {
        throw new ValidationException("macro values must not be negative");
      }
      validateKeyNutrient(command.fiberG(), "fiberG");
      validateKeyNutrient(command.sugarsG(), "sugarsG");
      if (command.sodiumMg() != null && command.sodiumMg() < 0) {
        throw new ValidationException("sodiumMg must not be negative");
      }
      validateKeyNutrient(command.saturatedFatG(), "saturatedFatG");
      NutritionTotals totals =
          new NutritionTotals(command.kcal(), command.proteinG(), command.carbsG(), command.fatG());
      KeyNutrientTotals keyNutrients =
          new KeyNutrientTotals(
              command.fiberG(), command.sugarsG(), command.sodiumMg(), command.saturatedFatG());
      entry =
          MealLogEntry.freeEntry(
              command.date(), command.mealType(), command.name(), totals, keyNutrients);
    } else {
      throw new ValidationException(
          "Provide either foodItemId+portions or free-item macros (name + kcal/proteinG/carbsG/fatG)");
    }

    return repository.save(currentUserProvider.currentUserId(), entry);
  }

  /**
   * The owner's day consumption read model for {@code date}: consumed macros derived fresh from
   * that day's logged entries, plus the date's resolved {@code target}/{@code comparison}
   * (FOR-128). Never 404s — an empty day returns zeroed consumption (spec FOR-127 edge case), with
   * {@code target}/{@code comparison} still populated from the resolved day type.
   *
   * @throws ValidationException if {@code date} is missing or far in the future
   */
  public DayConsumption consumption(LocalDate date) {
    validateDate(date);
    var stored = repository.findByOwnerAndDate(currentUserProvider.currentUserId(), date);
    MealLog log =
        stored.stream()
            .map(StoredMealLogEntry::entry)
            .reduce(MealLog.empty(date), MealLog::withEntry, (a, b) -> b);
    NutritionTotals consumed = log.consumedTotals();
    KeyNutrientTotals keyNutrients = log.consumedKeyNutrients();

    NutritionDayType dayType = NutritionDayTypeResolver.resolve(date);
    // The target comes from the user's ACTIVE PLAN rather than from constants in the jar (V53/V54).
    // Same shape of answer as before — a target for this kind of day, or null — but it is now a
    // target somebody can edit, and null is now a real state (no plan yet) rather than a fail-safe.
    MacroTargets target =
        planTargets.targetsForDayType(currentUserProvider.currentUserId(), dayType).orElse(null);
    TargetComparison comparison = TargetComparison.of(consumed, target);

    return new DayConsumption(date, dayType, consumed, keyNutrients, target, comparison, stored);
  }

  private void validateDate(LocalDate date) {
    if (date == null) {
      throw new ValidationException("date is required");
    }
    LocalDate maxAllowed = LocalDate.now(clock).plusDays(1);
    if (date.isAfter(maxAllowed)) {
      throw new ValidationException("date must not be in the far future");
    }
  }

  /** Validates an optional free-entry key nutrient (FOR-134): non-negative when present. */
  private static void validateKeyNutrient(Double value, String field) {
    if (value != null && value < 0) {
      throw new ValidationException(field + " must not be negative");
    }
  }
}

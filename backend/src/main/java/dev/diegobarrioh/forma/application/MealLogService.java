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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final PlannedDaySource planDays;
  private final PlannedMealOwnership plannedMeals;
  private final ServingLookup servings;

  public MealLogService(
      MealLogRepository repository,
      Clock clock,
      CurrentUserProvider currentUserProvider,
      FoodCatalogService foods,
      PlannedDaySource planDays,
      PlannedMealOwnership plannedMeals,
      ServingLookup servings) {
    this.repository = repository;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
    this.foods = foods;
    this.planDays = planDays;
    this.plannedMeals = plannedMeals;
    this.servings = servings;
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
      FoodItem food =
          foods
              .findById(command.foodItemId())
              .orElseThrow(
                  () -> new ValidationException("unknown foodItemId: " + command.foodItemId()));
      entry =
          MealLogEntry.fromCatalogGrams(
              command.date(), command.mealType(), food, (int) Math.round(gramsOf(command, food)));
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

    UUID userId = currentUserProvider.currentUserId();
    if (command.plannedMealId() != null) {
      // Checked here and not left to the foreign key: the database knows the row exists, not whose
      // it is. Without this, an entry could be attached to somebody else's planned meal and show up
      // in their adherence.
      if (!plannedMeals.ownsPlannedMeal(userId, command.plannedMealId())) {
        throw new ValidationException("No existe esa comida en tu plan.");
      }
      entry = entry.withPlannedMeal(command.plannedMealId());
    }
    return repository.save(userId, entry);
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
    Optional<ResolvedDay> planned =
        planDays.dayOfType(currentUserProvider.currentUserId(), dayType);
    MacroTargets target = planned.map(ResolvedDay::targets).orElse(null);
    TargetComparison comparison = TargetComparison.of(consumed, target);

    return new DayConsumption(
        date,
        dayType,
        consumed,
        keyNutrients,
        target,
        comparison,
        stored,
        adherence(planned.orElse(null), stored, date));
  }

  /** Removes every log answering one planned meal on one day for the current owner. */
  public void unmarkPlannedMeal(LocalDate date, UUID plannedMealId) {
    validateDate(date);
    if (plannedMealId == null) {
      throw new ValidationException("plannedMealId is required");
    }
    UUID userId = currentUserProvider.currentUserId();
    if (!plannedMeals.ownsPlannedMeal(userId, plannedMealId)) {
      throw new ValidationException("No existe esa comida en tu plan.");
    }
    repository.deleteByOwnerDateAndPlannedMeal(userId, date, plannedMealId);
  }

  /**
   * How many grams a logged catalog entry comes to.
   *
   * <p>Three ways to say the same thing, in the order somebody would mean them:
   *
   * <ul>
   *   <li>grams, which every food can be measured in;
   *   <li>a count of a NAMED portion of that food (V49) — "1 plátano mediano";
   *   <li>a count of the food's DEFAULT portion, which is all this endpoint understood until now.
   * </ul>
   *
   * <p>The same vocabulary the plan uses for its own lines, and deliberately: a planned meal and
   * the entry that answers it should be sayable the same way, or logging what the plan asked for
   * means translating it first.
   *
   * <p>A food with no portion at all used to be unloggable — {@code fromCatalog} threw, and nothing
   * caught it, so it reached the caller as a 500. Now it is loggable in grams, and asking for
   * portions of a food that has none is a sentence rather than a server error.
   */
  private double gramsOf(LogMealCommand command, FoodItem food) {
    if (command.grams() != null) {
      if (command.grams() <= 0) {
        throw new ValidationException("Los gramos deben ser mayores que cero.");
      }
      return command.grams();
    }
    if (command.portions() == null || command.portions() <= 0) {
      throw new ValidationException("Indica cuántos gramos o cuántas raciones.");
    }
    if (command.servingId() != null) {
      FoodServing serving =
          servings
              .find(command.servingId())
              .orElseThrow(
                  () -> new ValidationException("No existe la ración: " + command.servingId()));
      // A portion counts portions of ITS food. The same rule the plan enforces, for the same
      // reason: two slices of olive oil is arithmetically fine and means nothing.
      if (!serving.foodId().equals(food.id())) {
        throw new ValidationException(
            "La ración " + command.servingId() + " no es de " + food.id() + ".");
      }
      return command.portions() * serving.grams().doubleValue();
    }
    if (food.defaultServingG() == null) {
      throw new ValidationException(
          "Ese alimento no tiene ninguna ración definida; indica los gramos.");
    }
    return command.portions() * food.defaultServingG();
  }

  /**
   * Which of the day's planned meals have been eaten (V55).
   *
   * <p>Derived from the entries pointing at each one, never stored. PENDING and SKIPPED are the
   * same absence read against the clock: nothing logged for today is still to come, and nothing
   * logged for last tuesday was not eaten. A stored status would have needed somebody to turn the
   * first into the second at midnight.
   */
  private List<PlannedMealStatus> adherence(
      ResolvedDay planned, List<StoredMealLogEntry> entries, LocalDate date) {
    if (planned == null) {
      return List.of();
    }
    Set<String> eaten =
        entries.stream()
            .map(stored -> stored.entry().plannedMealId())
            .filter(Objects::nonNull)
            .map(UUID::toString)
            .collect(Collectors.toSet());
    boolean dayIsOver = date.isBefore(LocalDate.now(clock));
    return planned.meals().stream()
        .filter(meal -> meal.id() != null)
        .map(
            meal -> {
              PlannedMealStatus.State state =
                  eaten.contains(meal.id().toString())
                      ? PlannedMealStatus.State.EATEN
                      : dayIsOver
                          ? PlannedMealStatus.State.SKIPPED
                          : PlannedMealStatus.State.PENDING;
              return new PlannedMealStatus(
                  meal.id().toString(), meal.name(), meal.mealType(), meal.optional(), state);
            })
        .toList();
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

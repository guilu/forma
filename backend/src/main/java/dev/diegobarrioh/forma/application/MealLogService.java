package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MealLog;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.NutritionDayCatalog;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionDayTypeResolver;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Application use cases for meal consumption logging and the day consumption read model
 * (FOR-127/FOR-128, first implementable slices of FOR-102). Macros only (kcal/protein/carbs/fat);
 * hydration and key nutrients are later FOR-102 slices.
 *
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row, mirroring
 * {@link GoalService#OWNER_ID}/{@link UserProfileService#OWNER_ID} (FOR-107/125). Never logs entry
 * contents (personal health data) — see method javadoc.
 *
 * <p><b>Plan-target resolution (FOR-128).</b> {@link #consumption} resolves {@code date} to a
 * {@link NutritionDayType} via {@link NutritionDayTypeResolver} (which itself reuses the shared
 * training day-classification — no duplicated policy, no circular dependency on any training
 * service), looks up that type's {@link NutritionDayTemplate} in {@link NutritionDayCatalog}, and
 * compares it to the day's consumed totals via {@link TargetComparison#of}. {@code target}/{@code
 * comparison} are {@code null} only if the catalog has no template for the resolved type — a
 * fail-safe for a closed, always-seeded enum, not an expected runtime path.
 */
@Service
public class MealLogService {

  /** Fixed single-user owner id for the MVP (ADR-002), mirroring {@link GoalService#OWNER_ID}. */
  public static final String OWNER_ID = "default-user";

  private final MealLogRepository repository;
  private final Clock clock;

  public MealLogService(MealLogRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
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
          FoodCatalog.findById(command.foodItemId())
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
      NutritionTotals totals =
          new NutritionTotals(command.kcal(), command.proteinG(), command.carbsG(), command.fatG());
      entry = MealLogEntry.freeEntry(command.date(), command.mealType(), command.name(), totals);
    } else {
      throw new ValidationException(
          "Provide either foodItemId+portions or free-item macros (name + kcal/proteinG/carbsG/fatG)");
    }

    return repository.save(OWNER_ID, entry);
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
    var stored = repository.findByOwnerAndDate(OWNER_ID, date);
    MealLog log =
        stored.stream()
            .map(StoredMealLogEntry::entry)
            .reduce(MealLog.empty(date), MealLog::withEntry, (a, b) -> b);
    NutritionTotals consumed = log.consumedTotals();

    NutritionDayType dayType = NutritionDayTypeResolver.resolve(date);
    NutritionDayTemplate target =
        NutritionDayCatalog.findByType(dayType).map(day -> day.template()).orElse(null);
    TargetComparison comparison = target == null ? null : TargetComparison.of(consumed, target);

    return new DayConsumption(date, dayType, consumed, target, comparison, stored);
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
}

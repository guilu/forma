package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link MealLogService} (FOR-127/FOR-128): logging catalog/free
 * entries (owner-scoped, ADR-002), input validation, and the day consumption read model reusing the
 * shared per-item calculator plus the FOR-128 date -&gt; {@link NutritionDayType} resolver, with
 * the day's target coming from the caller's plan through {@link DayTargetSource} (V53/V54).
 * Hand-rolled in-memory fake (no Spring, no Mockito), matching {@code GoalServiceTest} (FOR-125).
 */
class MealLogServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC);

  // 2026-07-14 is a Tuesday -> STRENGTH day per the shared weekly training day policy (FOR-151).
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 14);
  private static final LocalDate A_SATURDAY = LocalDate.of(2026, 7, 11); // RUNNING (not future)
  private static final LocalDate A_FRIDAY = LocalDate.of(2026, 7, 10); // REST (not future)

  private static final UUID USER_ID = UUID.randomUUID();

  /**
   * A stand-in plan, one target per kind of day.
   *
   * <p>A lambda because {@link DayTargetSource} asks for one thing. Before V54 this came from
   * NutritionDayCatalog, three constants in the jar — which also meant this test could not describe
   * an account with no plan at all, because no such account could exist.
   */
  private static final MacroTargets STRENGTH_TARGET = new MacroTargets(2400, 170.0, 250.0, 70.0);

  private static final MacroTargets RUNNING_TARGET = new MacroTargets(2600, 160.0, 320.0, 65.0);

  private static final MacroTargets REST_TARGET = new MacroTargets(2100, 165.0, 190.0, 75.0);

  private static final PlannedDaySource PLAN_DAYS =
      (userId, type) ->
          java.util.Optional.of(
              new ResolvedDay(
                  type,
                  1,
                  1,
                  null,
                  null,
                  switch (type) {
                    case STRENGTH -> STRENGTH_TARGET;
                    case RUNNING -> RUNNING_TARGET;
                    case REST -> REST_TARGET;
                  },
                  new NutritionTotals(0, 0.0, 0.0, 0.0),
                  null,
                  java.util.List.of()));

  /** Nothing in any plan is the caller's, which is what an account with no plan looks like. */
  private static final PlannedMealOwnership OWNS_NOTHING = (userId, mealId) -> false;

  /** One named portion, to log "un plátano mediano" with. */
  private static final ServingLookup SERVINGS =
      id ->
          "banana-md".equals(id)
              ? java.util.Optional.of(
                  new FoodServing(
                      "banana-md", "banana", "Mediano", new java.math.BigDecimal("120.0"), true, 0))
              : java.util.Optional.empty();

  private final RecordingMealLogRepository repository = new RecordingMealLogRepository();
  private final MealLogService service =
      new MealLogService(
          repository,
          FIXED_CLOCK,
          () -> USER_ID,
          SeededFoodCatalog.service(),
          PLAN_DAYS,
          OWNS_NOTHING,
          SERVINGS);

  @Test
  void logsACatalogEntryResolvingFoodAndPortionsToMacrosViaTheCalculator() {
    LogMealCommand command = LogMealCommand.catalog(TODAY, MealType.LUNCH, "oats", 1.0); // 60g oats

    StoredMealLogEntry stored = service.log(command);

    assertThat(stored.id()).isNotBlank();
    assertThat(stored.entry().name()).isEqualTo("Copos de avena");
    assertThat(stored.entry().foodItemId()).isEqualTo("oats");
    assertThat(stored.entry().totals().calories()).isEqualTo(222); // 370 * 0.6
  }

  @Test
  void logsAFreeEntryStoringTheProvidedMacrosAsIs() {
    LogMealCommand command =
        LogMealCommand.free(TODAY, MealType.MID_MORNING, "Café con leche", 90, 5.0, 8.0, 3.0);

    StoredMealLogEntry stored = service.log(command);

    assertThat(stored.entry().foodItemId()).isNull();
    assertThat(stored.entry().totals()).isEqualTo(new NutritionTotals(90, 5.0, 8.0, 3.0));
  }

  @Test
  void rejectsAnEntryWithNeitherFoodItemIdNorMacros() {
    LogMealCommand command =
        new LogMealCommand(
            TODAY,
            MealType.LUNCH,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAnEntryWithBothFoodItemIdAndFreeMacros() {
    LogMealCommand command =
        new LogMealCommand(
            TODAY,
            MealType.LUNCH,
            "oats",
            1.0,
            null,
            null,
            "Avena",
            90,
            5.0,
            8.0,
            3.0,
            null,
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAnUnknownFoodItemId() {
    LogMealCommand command = LogMealCommand.catalog(TODAY, MealType.LUNCH, "ghost-food", 1.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsNegativePortions() {
    LogMealCommand command = LogMealCommand.catalog(TODAY, MealType.LUNCH, "oats", -1.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAFarFutureDate() {
    LogMealCommand command =
        LogMealCommand.free(TODAY.plusYears(1), MealType.LUNCH, "X", 90, 5.0, 8.0, 3.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void consumptionForADayWithNoLogsReturnsZeroedTotalsNeverAnError() {
    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.consumed()).isEqualTo(new NutritionTotals(0, 0.0, 0.0, 0.0));
    assertThat(consumption.entries()).isEmpty();
  }

  @Test
  void consumptionSumsAllLoggedEntriesForTheDay() {
    service.log(LogMealCommand.free(TODAY, MealType.BREAKFAST, "A", 100, 10.0, 10.0, 10.0));
    service.log(LogMealCommand.free(TODAY, MealType.LUNCH, "B", 200, 20.0, 20.0, 20.0));

    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.entries()).hasSize(2);
    assertThat(consumption.consumed().calories()).isEqualTo(300);
  }

  @Test
  void consumptionResolvesTheDayTypeFromTheDateViaTheSharedWeeklyTrainingDayPolicy() {
    DayConsumption strengthDay = service.consumption(TODAY);
    DayConsumption runningDay = service.consumption(A_SATURDAY);
    DayConsumption restDay = service.consumption(A_FRIDAY);

    assertThat(strengthDay.dayType()).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(runningDay.dayType()).isEqualTo(NutritionDayType.RUNNING);
    assertThat(restDay.dayType()).isEqualTo(NutritionDayType.REST);
  }

  @Test
  void consumptionOnAStrengthDayTakesItsTargetFromThePlansStrengthDay() {
    service.log(LogMealCommand.free(TODAY, MealType.BREAKFAST, "A", 100, 10.0, 10.0, 10.0));

    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.target()).isEqualTo(STRENGTH_TARGET);
    assertThat(consumption.comparison())
        .isEqualTo(TargetComparison.of(consumption.consumed(), STRENGTH_TARGET));
  }

  @Test
  void consumptionOnARestDayTakesItsTargetFromThePlansRestDay() {
    DayConsumption consumption = service.consumption(A_FRIDAY);

    assertThat(consumption.target()).isEqualTo(REST_TARGET);
    assertThat(consumption.comparison()).isNotNull();
  }

  @Test
  void emptyDayIsConsumedZeroedButTargetAndComparisonAreStillPopulatedFromTheResolvedDayType() {
    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.consumed()).isEqualTo(new NutritionTotals(0, 0.0, 0.0, 0.0));
    assertThat(consumption.entries()).isEmpty();
    assertThat(consumption.target()).isNotNull();
    assertThat(consumption.comparison()).isNotNull();
  }

  /**
   * An account with no plan reports what it ate and no target, rather than a target it never set.
   *
   * <p>Not reachable before V54: every account shared three constants compiled into the jar, so a
   * null target was documented as a fail-safe that should never happen. Now it is monday morning
   * for anybody who has just registered.
   */
  @Test
  void consumptionWithoutAnActivePlanReportsNoTargetAndNoComparison() {
    MealLogService planless =
        new MealLogService(
            repository,
            FIXED_CLOCK,
            () -> USER_ID,
            SeededFoodCatalog.service(),
            (userId, type) -> java.util.Optional.empty(),
            OWNS_NOTHING,
            SERVINGS);
    planless.log(LogMealCommand.free(TODAY, MealType.BREAKFAST, "A", 100, 10.0, 10.0, 10.0));

    DayConsumption consumption = planless.consumption(TODAY);

    assertThat(consumption.consumed().calories()).isEqualTo(100);
    assertThat(consumption.dayType()).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(consumption.target()).isNull();
    assertThat(consumption.comparison()).isNull();
  }

  @Test
  void consumptionOnlyReflectsTheOwnersEntries() {
    repository.rows.add(
        new OwnedEntry(
            UUID.randomUUID(),
            new StoredMealLogEntry(
                UUID.randomUUID().toString(),
                MealLogEntry.freeEntry(
                    TODAY, MealType.LUNCH, "Ajeno", new NutritionTotals(999, 99.0, 99.0, 99.0)))));

    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.entries()).isEmpty();
  }

  // --- FOR-134: consumed key-nutrient totals, reusing the catalog + the null/partial rule ---

  @Test
  void consumptionForADayWithNoLogsHasZeroedKeyNutrientTotalsNeverNull() {
    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.keyNutrients()).isEqualTo(KeyNutrientTotals.zero());
  }

  @Test
  void consumptionSumsKeyNutrientsFromACatalogEntry() {
    // oats: fiber 10.6/100g, defaultServingG=60 -> 1 portion = 60g -> x0.6.
    service.log(LogMealCommand.catalog(TODAY, MealType.BREAKFAST, "oats", 1.0));

    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.keyNutrients().fiberG())
        .isCloseTo(6.4, org.assertj.core.api.Assertions.within(0.05));
  }

  @Test
  void consumptionAcceptsOptionalKeyNutrientsOnAFreeEntry() {
    LogMealCommand command =
        new LogMealCommand(
            TODAY,
            MealType.MID_MORNING,
            null,
            null,
            null,
            null,
            "Barrita",
            180,
            6.0,
            24.0,
            7.0,
            3.0,
            12.0,
            90,
            2.0,
            null);

    service.log(command);
    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.keyNutrients()).isEqualTo(new KeyNutrientTotals(3.0, 12.0, 90, 2.0));
  }

  @Test
  void consumptionNullsAKeyNutrientTotalWhenOneLoggedEntryLacksIt() {
    LogMealCommand withFiber =
        new LogMealCommand(
            TODAY,
            MealType.BREAKFAST,
            null,
            null,
            null,
            null,
            "A",
            100,
            10.0,
            10.0,
            10.0,
            3.0,
            null,
            null,
            null,
            null);
    LogMealCommand withoutFiber =
        LogMealCommand.free(TODAY, MealType.LUNCH, "B", 100, 10.0, 10.0, 10.0); // no key nutrients

    service.log(withFiber);
    service.log(withoutFiber);
    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.keyNutrients().fiberG()).isNull();
  }

  @Test
  void rejectsANegativeFreeEntryFiber() {
    LogMealCommand command =
        new LogMealCommand(
            TODAY,
            MealType.LUNCH,
            null,
            null,
            null,
            null,
            "X",
            90,
            5.0,
            8.0,
            3.0,
            -1.0,
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsANegativeFreeEntrySodium() {
    LogMealCommand command =
        new LogMealCommand(
            TODAY,
            MealType.LUNCH,
            null,
            null,
            null,
            null,
            "X",
            90,
            5.0,
            8.0,
            3.0,
            null,
            null,
            -1,
            null,
            null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  /** In-memory fake, matching {@code RecordingGoalRepository} (FOR-125). */
  private static class RecordingMealLogRepository implements MealLogRepository {
    final List<OwnedEntry> rows = new ArrayList<>();

    @Override
    public List<StoredMealLogEntry> findByOwnerAndDate(UUID userId, LocalDate date) {
      return rows.stream()
          .filter(r -> r.userId.equals(userId) && r.stored.entry().date().equals(date))
          .map(r -> r.stored)
          .toList();
    }

    @Override
    public StoredMealLogEntry save(UUID userId, MealLogEntry entry) {
      StoredMealLogEntry stored = new StoredMealLogEntry(UUID.randomUUID().toString(), entry);
      rows.add(new OwnedEntry(userId, stored));
      return stored;
    }
  }

  private record OwnedEntry(UUID userId, StoredMealLogEntry stored) {}

  // --- Ways of saying how much (V49 portions reached the log at last) ---

  /** Grams, which every food can be measured in — including one with no portion recorded at all. */
  @Test
  void logsACatalogEntryByGrams() {
    StoredMealLogEntry stored =
        service.log(LogMealCommand.catalogGrams(TODAY, MealType.LUNCH, "oats", 120));

    assertThat(stored.entry().totals().calories()).isEqualTo(444); // 370 * 1.2
  }

  /**
   * A named portion, which is how anybody actually says it: one medium banana, not 1.5 servings.
   */
  @Test
  void logsACatalogEntryByANamedPortion() {
    StoredMealLogEntry stored =
        service.log(
            LogMealCommand.catalogServings(TODAY, MealType.SNACK, "banana", "banana-md", 1));

    // 120 g of banana at 89 kcal/100 g.
    assertThat(stored.entry().totals().calories()).isEqualTo(107);
  }

  /** A portion counts portions of ITS food; two slices of oats is not a thing. */
  @Test
  void refusesAPortionThatBelongsToAnotherFood() {
    assertThatThrownBy(
            () ->
                service.log(
                    LogMealCommand.catalogServings(TODAY, MealType.LUNCH, "oats", "banana-md", 1)))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("no es de oats");
  }

  @Test
  void refusesAPortionNobodyWrote() {
    assertThatThrownBy(
            () ->
                service.log(
                    LogMealCommand.catalogServings(TODAY, MealType.LUNCH, "oats", "fantasma", 1)))
        .isInstanceOf(ValidationException.class);
  }

  /** Neither grams nor portions is not an amount. */
  @Test
  void refusesACatalogEntryWithNoAmountAtAll() {
    assertThatThrownBy(
            () ->
                service.log(
                    new LogMealCommand(
                        TODAY,
                        MealType.LUNCH,
                        "oats",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(ValidationException.class);
  }
}

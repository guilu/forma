package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayCatalog;
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
 * entries (owner-scoped, ADR-002), input validation, and the day consumption read model reusing
 * {@link NutritionCalculationService}-style calculators plus the FOR-128 date -&gt; {@link
 * NutritionDayType} resolver so {@code target}/{@code comparison} populate. Hand-rolled in-memory
 * fake (no Spring, no Mockito), matching {@code GoalServiceTest} (FOR-125).
 */
class MealLogServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);

  // 2026-07-15 is a Wednesday -> STRENGTH day per the shared weekly training day policy.
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
  private static final LocalDate A_SATURDAY = LocalDate.of(2026, 7, 11); // RUNNING (not future)
  private static final LocalDate A_SUNDAY = LocalDate.of(2026, 7, 12); // REST (not future)

  private final RecordingMealLogRepository repository = new RecordingMealLogRepository();
  private final MealLogService service = new MealLogService(repository, FIXED_CLOCK);

  @Test
  void logsACatalogEntryResolvingFoodAndPortionsToMacrosViaTheCalculator() {
    LogMealCommand command = LogMealCommand.catalog(TODAY, MealType.LUNCH, "oats", 1.0); // 60g oats

    StoredMealLogEntry stored = service.log(command);

    assertThat(stored.id()).isNotBlank();
    assertThat(stored.entry().name()).isEqualTo("Avena");
    assertThat(stored.entry().foodItemId()).isEqualTo("oats");
    assertThat(stored.entry().totals().calories()).isEqualTo(233); // 389 * 0.6
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
        new LogMealCommand(TODAY, MealType.LUNCH, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAnEntryWithBothFoodItemIdAndFreeMacros() {
    LogMealCommand command =
        new LogMealCommand(TODAY, MealType.LUNCH, "oats", 1.0, "Avena", 90, 5.0, 8.0, 3.0);

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
    DayConsumption restDay = service.consumption(A_SUNDAY);

    assertThat(strengthDay.dayType()).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(runningDay.dayType()).isEqualTo(NutritionDayType.RUNNING);
    assertThat(restDay.dayType()).isEqualTo(NutritionDayType.REST);
  }

  @Test
  void consumptionOnAStrengthDayPopulatesTargetAndComparisonFromTheStrengthTemplate() {
    service.log(LogMealCommand.free(TODAY, MealType.BREAKFAST, "A", 100, 10.0, 10.0, 10.0));

    DayConsumption consumption = service.consumption(TODAY);

    var expectedTemplate = NutritionDayCatalog.findByType(NutritionDayType.STRENGTH).orElseThrow();
    assertThat(consumption.target()).isEqualTo(expectedTemplate.template());
    assertThat(consumption.comparison())
        .isEqualTo(TargetComparison.of(consumption.consumed(), expectedTemplate.template()));
  }

  @Test
  void consumptionOnARestDayPopulatesTargetFromTheRestTemplate() {
    DayConsumption consumption = service.consumption(A_SUNDAY);

    var expectedTemplate = NutritionDayCatalog.findByType(NutritionDayType.REST).orElseThrow();
    assertThat(consumption.target()).isEqualTo(expectedTemplate.template());
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

  @Test
  void consumptionOnlyReflectsTheOwnersEntries() {
    repository.rows.add(
        new OwnedEntry(
            "other-user",
            new StoredMealLogEntry(
                UUID.randomUUID().toString(),
                MealLogEntry.freeEntry(
                    TODAY, MealType.LUNCH, "Ajeno", new NutritionTotals(999, 99.0, 99.0, 99.0)))));

    DayConsumption consumption = service.consumption(TODAY);

    assertThat(consumption.entries()).isEmpty();
  }

  /** In-memory fake, matching {@code RecordingGoalRepository} (FOR-125). */
  private static class RecordingMealLogRepository implements MealLogRepository {
    final List<OwnedEntry> rows = new ArrayList<>();

    @Override
    public List<StoredMealLogEntry> findByOwnerAndDate(String ownerId, LocalDate date) {
      return rows.stream()
          .filter(r -> r.ownerId.equals(ownerId) && r.stored.entry().date().equals(date))
          .map(r -> r.stored)
          .toList();
    }

    @Override
    public StoredMealLogEntry save(String ownerId, MealLogEntry entry) {
      StoredMealLogEntry stored = new StoredMealLogEntry(UUID.randomUUID().toString(), entry);
      rows.add(new OwnedEntry(ownerId, stored));
      return stored;
    }
  }

  private record OwnedEntry(String ownerId, StoredMealLogEntry stored) {}
}

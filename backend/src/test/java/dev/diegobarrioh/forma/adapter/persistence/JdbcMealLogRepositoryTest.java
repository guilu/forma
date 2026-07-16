package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.MealLogRepository;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcMealLogRepository} (FOR-127). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V13), like the FOR-107/FOR-125 tests.
 * Covers the round-trip and empty-database fixtures from tests.md.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcMealLogRepositoryTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);
  private static final LocalDate OTHER_DAY = LocalDate.of(2026, 7, 16);

  @Autowired private MealLogRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM meal_log_entry");
  }

  @Test
  void findByOwnerAndDateIsEmptyOnACleanDatabase() {
    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }

  @Test
  void savedEntryRoundTripsThroughFindByOwnerAndDate() {
    MealLogEntry entry =
        MealLogEntry.freeEntry(
            DAY, MealType.LUNCH, "Café con leche", new NutritionTotals(90, 5.0, 8.0, 3.0));

    StoredMealLogEntry created = repository.save(OWNER, entry);
    List<StoredMealLogEntry> found = repository.findByOwnerAndDate(OWNER, DAY);

    assertThat(created.id()).isNotBlank();
    assertThat(found).hasSize(1);
    StoredMealLogEntry read = found.get(0);
    assertThat(read.id()).isEqualTo(created.id());
    assertThat(read.entry().date()).isEqualTo(DAY);
    assertThat(read.entry().mealType()).isEqualTo(MealType.LUNCH);
    assertThat(read.entry().name()).isEqualTo("Café con leche");
    assertThat(read.entry().foodItemId()).isNull();
    assertThat(read.entry().totals()).isEqualTo(new NutritionTotals(90, 5.0, 8.0, 3.0));
  }

  @Test
  void aCatalogEntryPersistsItsFoodItemId() {
    MealLogEntry entry =
        MealLogEntry.freeEntry(
            DAY, MealType.BREAKFAST, "Avena", new NutritionTotals(233, 10.1, 39.8, 4.1));
    MealLogEntry catalogEntry =
        new MealLogEntry(DAY, MealType.BREAKFAST, "Avena", "oats", entry.totals());

    repository.save(OWNER, catalogEntry);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    assertThat(read.entry().foodItemId()).isEqualTo("oats");
  }

  @Test
  void multipleEntriesSameDayAreAllReturnedNeverOverwritten() {
    repository.save(
        OWNER,
        MealLogEntry.freeEntry(
            DAY, MealType.BREAKFAST, "A", new NutritionTotals(100, 1.0, 1.0, 1.0)));
    repository.save(
        OWNER,
        MealLogEntry.freeEntry(DAY, MealType.LUNCH, "B", new NutritionTotals(200, 2.0, 2.0, 2.0)));

    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).hasSize(2);
  }

  @Test
  void findByOwnerAndDateNeverReturnsAnotherOwnersEntries() {
    repository.save(
        OTHER_OWNER,
        MealLogEntry.freeEntry(
            DAY, MealType.LUNCH, "Ajeno", new NutritionTotals(1, 1.0, 1.0, 1.0)));

    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }

  @Test
  void findByOwnerAndDateNeverReturnsEntriesForAnotherDate() {
    repository.save(
        OWNER,
        MealLogEntry.freeEntry(
            OTHER_DAY, MealType.LUNCH, "OtroDia", new NutritionTotals(1, 1.0, 1.0, 1.0)));

    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }

  /**
   * FOR-134 known limitation, documented rather than silently lost: {@code meal_log_entry} (V13)
   * has no key-nutrient columns, and this story adds no migration (head stays V16, reference data
   * only). A snapshot computed at logging time is therefore NOT persisted, so a JDBC round trip
   * always reconstructs {@link KeyNutrientTotals#empty()} for a reloaded entry, even when the
   * original entry (in-memory, pre-persistence) carried known key nutrients. This is honest (never
   * fabricated) rather than a silent precision loss, and is called out in the PR's "Known
   * limitations" — a follow-up story adding a migration is needed to persist per-entry key
   * nutrients.
   */
  @Test
  void keyNutrientsAreNotPersistedYetAndReloadAsEmptyNoMigrationInThisSlice() {
    var oats = FoodCatalog.findById("oats").orElseThrow(); // has known key nutrients
    MealLogEntry original = MealLogEntry.fromCatalog(DAY, MealType.BREAKFAST, oats, 1.0);
    assertThat(original.keyNutrients()).isNotEqualTo(KeyNutrientTotals.empty());

    repository.save(OWNER, original);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    assertThat(read.entry().keyNutrients()).isEqualTo(KeyNutrientTotals.empty());
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.MealLogRepository;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.FoodCatalog;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 *
 * <p>FOR-145b-1 (migration V27): {@code meal_log_entry.user_id} FK-references {@code users(id)}, so
 * {@code OTHER_OWNER} must be a real seeded row.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcMealLogRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();
  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);
  private static final LocalDate OTHER_DAY = LocalDate.of(2026, 7, 16);

  @Autowired private MealLogRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM meal_log_entry");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "meallog-other-owner@test.local",
        "!");
  }

  /**
   * Leaves no live {@code meal_log_entry} rows referencing {@code OTHER_OWNER} after the last test
   * in this class runs (ADR-007 shared named in-memory H2 across the whole test run) — otherwise a
   * later test class that blanket-deletes non-placeholder {@code users} rows (e.g. {@code
   * AuthenticationFlowIntegrationTest#clearTestUsers}) would hit an FK violation.
   */
  @AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM meal_log_entry");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
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
   * FOR-134 (migration V17): a catalog entry's key nutrients, snapshotted at logging time from the
   * FOR-30 {@link FoodCatalog} food, survive a full JDBC round trip — proving they are genuinely
   * persisted to and read back from the {@code meal_log_entry} key-nutrient columns, not just held
   * in memory. Oats carries all four values (fibre/sugars/sodium/saturated fat), so none is lost.
   */
  @Test
  void aCatalogEntrysKeyNutrientsSurviveAFullPersistenceRoundTrip() {
    var oats = FoodCatalog.findById("oats").orElseThrow(); // all four key nutrients known
    MealLogEntry original = MealLogEntry.fromCatalog(DAY, MealType.BREAKFAST, oats, 1.0);
    assertThat(original.keyNutrients()).isNotEqualTo(KeyNutrientTotals.empty());

    repository.save(OWNER, original);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    assertThat(read.entry().keyNutrients()).isEqualTo(original.keyNutrients());
  }

  /**
   * FOR-134 (migration V17): a free entry's optionally-provided key nutrients (sodium in mg, others
   * in grams) round-trip exactly, and the sodium value keeps its milligram magnitude (a NUMERIC
   * column, not truncated or unit-swapped).
   */
  @Test
  void aFreeEntrysProvidedKeyNutrientsSurviveAFullPersistenceRoundTrip() {
    KeyNutrientTotals provided = new KeyNutrientTotals(3.0, 12.0, 90, 2.0);
    MealLogEntry original =
        MealLogEntry.freeEntry(
            DAY,
            MealType.MID_MORNING,
            "Barrita",
            new NutritionTotals(180, 6.0, 24.0, 7.0),
            provided);

    repository.save(OWNER, original);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    assertThat(read.entry().keyNutrients()).isEqualTo(provided);
    assertThat(read.entry().keyNutrients().sodiumMg()).isEqualTo(90);
  }

  /**
   * FOR-134 (migration V17): a partially-known key-nutrient profile round-trips with its nulls
   * intact — a nutrient the food genuinely lacks stays {@code null} through persistence (never
   * fabricated as 0), independently per column. "chicken" has fibre/sugars = 0 but sodium and sat
   * fat = null (FOR-152: not given by the Macros sheet).
   */
  @Test
  void partialKeyNutrientNullsAreNotFabricatedThroughPersistence() {
    var chicken = FoodCatalog.findById("chicken").orElseThrow(); // sodium and sat fat null
    MealLogEntry original = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, chicken, 1.0);

    repository.save(OWNER, original);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    KeyNutrientTotals keyNutrients = read.entry().keyNutrients();
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients).isEqualTo(original.keyNutrients());
  }

  /**
   * FOR-134 (migration V17): a free entry with no key nutrients (all columns NULL) round-trips as
   * {@link KeyNutrientTotals#empty()}, and the row still loads cleanly — the backward-compatible
   * nullable-columns case (also what any pre-V17 row backfills to).
   */
  @Test
  void anEntryWithNoKeyNutrientsRoundTripsAsEmpty() {
    MealLogEntry original =
        MealLogEntry.freeEntry(
            DAY, MealType.LUNCH, "Café con leche", new NutritionTotals(90, 5.0, 8.0, 3.0));
    assertThat(original.keyNutrients()).isEqualTo(KeyNutrientTotals.empty());

    repository.save(OWNER, original);
    StoredMealLogEntry read = repository.findByOwnerAndDate(OWNER, DAY).get(0);

    assertThat(read.entry().keyNutrients()).isEqualTo(KeyNutrientTotals.empty());
  }
}

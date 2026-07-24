package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.support.AuthTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end integration test (FOR-134, migration V17): proves consumed key-nutrient totals flow
 * through the REAL database — {@link MealLogService} logs entries via the real {@link
 * dev.diegobarrioh.forma.adapter.persistence.JdbcMealLogRepository}, and {@link
 * MealLogService#consumption} reads them back and sums them. Unlike {@code MealLogServiceTest}
 * (in-memory fake repository), this exercises the actual JDBC persist + reload of the key-nutrient
 * columns against the in-memory PostgreSQL-mode H2 with the full Flyway chain (V1..V17) applied,
 * matching {@code JdbcMealLogRepositoryTest}'s style.
 */
@SpringBootTest
@ActiveProfiles("test")
class MealLogConsumptionPersistenceTest {

  // A past date (fixed relative to the systemDefaultZone clock) so validation never rejects it.
  private static final LocalDate DAY = LocalDate.of(2026, 1, 15);

  @Autowired private MealLogService service;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM meal_log_entry");
    AuthTestSupport.authenticateThreadAsPlaceholderUser();
  }

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void consumedKeyNutrientsReflectAFreeEntryPersistedThroughTheRealDatabase() {
    service.log(
        new LogMealCommand(
            DAY,
            MealType.MID_MORNING,
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
            2.0));

    DayConsumption consumption = service.consumption(DAY);

    assertThat(consumption.keyNutrients()).isEqualTo(new KeyNutrientTotals(3.0, 12.0, 90, 2.0));
  }

  @Test
  void consumedKeyNutrientsSumTwoPersistedCatalogEntriesReadBackFromTheDatabase() {
    // oats (all four known) logged twice -> the day total is a real sum of reloaded rows.
    service.log(LogMealCommand.catalog(DAY, MealType.BREAKFAST, "oats", 1.0));
    service.log(LogMealCommand.catalog(DAY, MealType.LUNCH, "oats", 1.0));

    DayConsumption consumption = service.consumption(DAY);

    // oats 1 portion = 60g -> fibre 10.6 * 0.6 = 6.36 -> 6.4; two entries -> 12.8.
    assertThat(consumption.keyNutrients().fiberG()).isEqualTo(12.8);
    assertThat(consumption.keyNutrients().sugarsG()).isEqualTo(0.0);
    assertThat(consumption.keyNutrients().sodiumMg()).isEqualTo(2); // 1mg per entry (2*0.6->1)
    assertThat(consumption.keyNutrients().saturatedFatG()).isEqualTo(1.4); // 0.7 * 2
  }

  @Test
  void aDayMixingAPersistedEntryWithoutFiberNullsTheFiberTotalPerTheDocumentedRule() {
    // "vegetables" has all key nutrients null; oats has them known. Persist both, read back:
    // the null/partial rule must hold across the REAL reloaded rows, not just in memory.
    service.log(LogMealCommand.catalog(DAY, MealType.BREAKFAST, "oats", 1.0));
    service.log(LogMealCommand.catalog(DAY, MealType.LUNCH, "vegetables", 1.0));

    DayConsumption consumption = service.consumption(DAY);

    assertThat(consumption.keyNutrients().fiberG()).isNull();
    assertThat(consumption.keyNutrients().sugarsG()).isNull();
    assertThat(consumption.keyNutrients().sodiumMg()).isNull();
    assertThat(consumption.keyNutrients().saturatedFatG()).isNull();
  }

  @Test
  void anEmptyDayHasZeroedKeyNutrientTotalsNeverNull() {
    DayConsumption consumption = service.consumption(DAY);

    assertThat(consumption.keyNutrients()).isEqualTo(KeyNutrientTotals.zero());
  }
}

package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.StoredWaterIntakeEntry;
import dev.diegobarrioh.forma.application.WaterIntakeRepository;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcWaterIntakeRepository} (FOR-130). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V14), like the FOR-127 {@code
 * JdbcMealLogRepositoryTest}. Covers the round-trip and empty-database fixtures from tests.md.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcWaterIntakeRepositoryTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);
  private static final LocalDate OTHER_DAY = LocalDate.of(2026, 7, 16);

  @Autowired private WaterIntakeRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM water_intake_entry");
  }

  @Test
  void findByOwnerAndDateIsEmptyOnACleanDatabase() {
    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }

  @Test
  void savedEntryRoundTripsThroughFindByOwnerAndDate() {
    WaterIntakeEntry entry = new WaterIntakeEntry(DAY, 500.0);

    StoredWaterIntakeEntry created = repository.save(OWNER, entry);
    List<StoredWaterIntakeEntry> found = repository.findByOwnerAndDate(OWNER, DAY);

    assertThat(created.id()).isNotBlank();
    assertThat(found).hasSize(1);
    StoredWaterIntakeEntry read = found.get(0);
    assertThat(read.id()).isEqualTo(created.id());
    assertThat(read.entry().date()).isEqualTo(DAY);
    assertThat(read.entry().volumeMl()).isEqualTo(500.0);
  }

  @Test
  void multipleEntriesSameDayAreAllReturnedNeverOverwritten() {
    repository.save(OWNER, new WaterIntakeEntry(DAY, 500.0));
    repository.save(OWNER, new WaterIntakeEntry(DAY, 300.0));
    repository.save(OWNER, new WaterIntakeEntry(DAY, 200.0));

    List<StoredWaterIntakeEntry> found = repository.findByOwnerAndDate(OWNER, DAY);

    assertThat(found).hasSize(3);
    double total = found.stream().mapToDouble(e -> e.entry().volumeMl()).sum();
    assertThat(total).isEqualTo(1000.0);
  }

  @Test
  void findByOwnerAndDateNeverReturnsAnotherOwnersEntries() {
    repository.save(OTHER_OWNER, new WaterIntakeEntry(DAY, 500.0));

    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }

  @Test
  void findByOwnerAndDateNeverReturnsEntriesForAnotherDate() {
    repository.save(OWNER, new WaterIntakeEntry(OTHER_DAY, 500.0));

    assertThat(repository.findByOwnerAndDate(OWNER, DAY)).isEmpty();
  }
}

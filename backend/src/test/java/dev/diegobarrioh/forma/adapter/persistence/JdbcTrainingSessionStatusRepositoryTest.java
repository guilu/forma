package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.TrainingSessionStatusRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcTrainingSessionStatusRepository} (FOR-27). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like the FOR-16 test.
 *
 * <p>FOR-145c (migration V31): the primary key is now the composite {@code (user_id, session_id)},
 * so every call is scoped through the always-present legacy placeholder account.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcTrainingSessionStatusRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;

  @Autowired private TrainingSessionStatusRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM training_session_status");
  }

  @Test
  void insertsThenReadsBackStatus() {
    repository.upsert(OWNER, "SATURDAY:RUNNING", SessionStatus.COMPLETED, "Hecho");

    var stored = repository.findAllByUser(OWNER);
    assertThat(stored).containsKey("SATURDAY:RUNNING");
    assertThat(stored.get("SATURDAY:RUNNING").status()).isEqualTo(SessionStatus.COMPLETED);
    assertThat(stored.get("SATURDAY:RUNNING").notes()).isEqualTo("Hecho");
  }

  @Test
  void upsertUpdatesAnExistingRowInPlace() {
    repository.upsert(OWNER, "MONDAY:STRENGTH", SessionStatus.COMPLETED, "v1");
    repository.upsert(OWNER, "MONDAY:STRENGTH", SessionStatus.SKIPPED, null);

    var stored = repository.findAllByUser(OWNER);
    // Still a single row, updated in place.
    assertThat(stored).hasSize(1);
    assertThat(stored.get("MONDAY:STRENGTH").status()).isEqualTo(SessionStatus.SKIPPED);
    assertThat(stored.get("MONDAY:STRENGTH").notes()).isNull();
  }
}

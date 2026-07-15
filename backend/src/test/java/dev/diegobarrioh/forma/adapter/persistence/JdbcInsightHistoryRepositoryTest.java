package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.InsightHistoryRepository;
import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcInsightHistoryRepository} (FOR-110). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like the FOR-107 profile test.
 * Covers the persist-on-generate, history-listing and prior-period lookup fixtures from tests.md.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcInsightHistoryRepositoryTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-07-13T08:00:00Z");
  private static final LocalDate WEEK_1 = LocalDate.of(2026, 6, 29);
  private static final LocalDate WEEK_2 = LocalDate.of(2026, 7, 6);
  private static final LocalDate WEEK_3 = LocalDate.of(2026, 7, 13);

  @Autowired private InsightHistoryRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTables() {
    jdbcTemplate.update("DELETE FROM insight_history_recommendation");
    jdbcTemplate.update("DELETE FROM insight_history");
  }

  private static Recommendation recommendation(String message) {
    return new Recommendation(
        GENERATED_AT,
        RecommendationCategory.BODY,
        RecommendationSeverity.ACTION,
        message,
        "reason text",
        "weeklyWeightChangeKg");
  }

  private static WeeklyInsights insightsFor(LocalDate week, double weightKg) {
    WeeklyCheckIn checkIn = new WeeklyCheckIn(week, weightKg, 18.0, 55.0, 3, 3, 3, 2, "notes");
    Recommendation main = recommendation("main message");
    Recommendation secondary = recommendation("secondary message");
    return new WeeklyInsights(checkIn, main, List.of(secondary), GENERATED_AT);
  }

  @Test
  void listAllReturnsEmptyBeforeAnyInsightsHaveBeenGenerated() {
    assertThat(repository.listAll()).isEmpty();
  }

  @Test
  void findMostRecentCheckInBeforeReturnsEmptyOnACleanDatabase() {
    assertThat(repository.findMostRecentCheckInBefore(WEEK_2)).isEmpty();
  }

  @Test
  void savesAndReadsBackAFullInsightsRoundTrip() {
    repository.save(insightsFor(WEEK_2, 72.0));

    List<WeeklyInsights> history = repository.listAll();

    assertThat(history).hasSize(1);
    WeeklyInsights stored = history.get(0);
    assertThat(stored.checkIn().weekStartDate()).isEqualTo(WEEK_2);
    assertThat(stored.checkIn().latestWeightKg()).isEqualTo(72.0);
    assertThat(stored.checkIn().latestBodyFatPercentage()).isEqualTo(18.0);
    assertThat(stored.checkIn().latestLeanMassKg()).isEqualTo(55.0);
    assertThat(stored.checkIn().plannedRunningSessions()).isEqualTo(3);
    assertThat(stored.checkIn().completedRunningSessions()).isEqualTo(3);
    assertThat(stored.checkIn().plannedStrengthSessions()).isEqualTo(3);
    assertThat(stored.checkIn().completedStrengthSessions()).isEqualTo(2);
    assertThat(stored.checkIn().notes()).isEqualTo("notes");
    assertThat(stored.main().message()).isEqualTo("main message");
    assertThat(stored.main().category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(stored.main().severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(stored.main().relatedMetric()).isEqualTo("weeklyWeightChangeKg");
    assertThat(stored.secondary()).hasSize(1);
    assertThat(stored.secondary().get(0).message()).isEqualTo("secondary message");
    assertThat(stored.generatedAt()).isEqualTo(GENERATED_AT);
  }

  @Test
  void listAllOrdersMostRecentPeriodFirst() {
    repository.save(insightsFor(WEEK_1, 74.0));
    repository.save(insightsFor(WEEK_3, 70.0));
    repository.save(insightsFor(WEEK_2, 72.0));

    List<WeeklyInsights> history = repository.listAll();

    assertThat(history)
        .extracting(insights -> insights.checkIn().weekStartDate())
        .containsExactly(WEEK_3, WEEK_2, WEEK_1);
  }

  @Test
  void findMostRecentCheckInBeforeSkipsAGapWeekAndFindsTheMostRecentPriorPeriod() {
    repository.save(insightsFor(WEEK_1, 74.0));
    // WEEK_2 is deliberately never generated (a gap week) — WEEK_3's prior must still be WEEK_1.
    repository.save(insightsFor(WEEK_3, 70.0));

    Optional<WeeklyCheckIn> prior = repository.findMostRecentCheckInBefore(WEEK_3);

    assertThat(prior).isPresent();
    assertThat(prior.get().weekStartDate()).isEqualTo(WEEK_1);
    assertThat(prior.get().latestWeightKg()).isEqualTo(74.0);
  }

  @Test
  void savingTheSamePeriodTwiceOverwritesRatherThanAppending() {
    repository.save(insightsFor(WEEK_2, 72.0));
    repository.save(insightsFor(WEEK_2, 71.0));

    List<WeeklyInsights> history = repository.listAll();

    assertThat(history).hasSize(1);
    assertThat(history.get(0).checkIn().latestWeightKg()).isEqualTo(71.0);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM insight_history_recommendation WHERE week_start_date = ?",
                Integer.class,
                WEEK_2))
        .isEqualTo(2);
  }
}

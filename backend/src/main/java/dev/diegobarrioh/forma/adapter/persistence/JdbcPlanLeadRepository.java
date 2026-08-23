package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.PlanLead;
import dev.diegobarrioh.forma.application.PlanLeadRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for {@code plan_lead} (migration V61). Plain {@link JdbcTemplate}, no ORM (ADR-003).
 *
 * <p>Nothing here logs the row it writes. The funnel's whole payload is personal data — a name, an
 * email and the body measurements somebody gave to get a plan — and a log line is a second copy
 * with a different retention period, a different access list and no way to honour a deletion
 * request against it.
 */
@Repository
public class JdbcPlanLeadRepository implements PlanLeadRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO plan_lead (
          id, full_name, email, country, heard_about_us,
          sex, age_years, weight_kg, height_cm, activity_level, objective,
          days_per_week, meals_per_day, eating_style, plan_kcal,
          accepts_privacy_policy, privacy_policy_version, wants_marketing, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?, ?)
      """;

  private static final String DELETE_OLDER_SQL = "DELETE FROM plan_lead WHERE created_at < ?";

  private final JdbcTemplate jdbcTemplate;

  public JdbcPlanLeadRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(PlanLead lead, Instant at) {
    jdbcTemplate.update(
        INSERT_SQL,
        lead.id(),
        lead.fullName(),
        lead.email(),
        lead.country(),
        lead.heardAboutUs(),
        lead.sex().name(),
        lead.ageYears(),
        lead.weightKg(),
        lead.heightCm(),
        lead.activityLevel().name(),
        lead.objective().name(),
        lead.daysPerWeek(),
        lead.mealsPerDay(),
        lead.eatingStyle(),
        lead.planKcal(),
        lead.privacyPolicyVersion(),
        lead.wantsMarketing(),
        OffsetDateTime.ofInstant(at, ZoneOffset.UTC));
  }

  @Override
  public int deleteOlderThan(Instant cutoff) {
    return jdbcTemplate.update(DELETE_OLDER_SQL, OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC));
  }
}

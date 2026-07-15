package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.UserProfileRepository;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.DistanceUnit;
import dev.diegobarrioh.forma.domain.EnergyUnit;
import dev.diegobarrioh.forma.domain.HeightUnit;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
import dev.diegobarrioh.forma.domain.WeightUnit;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists the {@link UserProfile} aggregate to the single-row-per-owner {@code
 * user_profile} table (FOR-107).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — the project has no JPA/ORM on purpose ({@code
 * backend/build.gradle}, same as {@link JdbcBodyMeasurementRepository}). {@code save} is a portable
 * update-then-insert upsert, following {@link JdbcTrainingSessionStatusRepository}'s pattern (works
 * on both PostgreSQL and the H2 test database).
 *
 * <p>The onboarding draft's {@code training.days}/{@code equipment.items} lists are stored as a
 * single comma-joined column each (no array/JSON column exists elsewhere in this schema, see V8
 * migration comment) — day/equipment labels are short controlled-vocabulary codes with no commas,
 * so this is safe for the MVP; a genuinely free-text multi-value field would need a real join
 * table.
 */
@Repository
public class JdbcUserProfileRepository implements UserProfileRepository {

  private static final String FIND_SQL =
      """
      SELECT owner_id, name, email, birth_date, sex, height_cm, activity_level, main_goal,
        weight_unit, height_unit, distance_unit, energy_unit,
        caloric_deficit_kcal, protein_target_g, daily_water_ml, theme_mode,
        onboarding_profile_name, onboarding_profile_birth_date, onboarding_profile_sex,
        onboarding_profile_height_cm, onboarding_metrics_choice, onboarding_metrics_saved,
        onboarding_goal_selected, onboarding_training_days, onboarding_equipment_items,
        onboarding_nutrition_preference, onboarding_nutrition_restrictions, first_run_completed
      FROM user_profile
      WHERE owner_id = ?
      """;

  private static final String UPDATE_SQL =
      """
      UPDATE user_profile SET
        name = ?, email = ?, birth_date = ?, sex = ?, height_cm = ?, activity_level = ?,
        main_goal = ?, weight_unit = ?, height_unit = ?, distance_unit = ?, energy_unit = ?,
        caloric_deficit_kcal = ?, protein_target_g = ?, daily_water_ml = ?, theme_mode = ?,
        onboarding_profile_name = ?, onboarding_profile_birth_date = ?, onboarding_profile_sex = ?,
        onboarding_profile_height_cm = ?, onboarding_metrics_choice = ?,
        onboarding_metrics_saved = ?, onboarding_goal_selected = ?, onboarding_training_days = ?,
        onboarding_equipment_items = ?, onboarding_nutrition_preference = ?,
        onboarding_nutrition_restrictions = ?, first_run_completed = ?
      WHERE owner_id = ?
      """;

  private static final String INSERT_SQL =
      """
      INSERT INTO user_profile
        (owner_id, name, email, birth_date, sex, height_cm, activity_level, main_goal,
         weight_unit, height_unit, distance_unit, energy_unit,
         caloric_deficit_kcal, protein_target_g, daily_water_ml, theme_mode,
         onboarding_profile_name, onboarding_profile_birth_date, onboarding_profile_sex,
         onboarding_profile_height_cm, onboarding_metrics_choice, onboarding_metrics_saved,
         onboarding_goal_selected, onboarding_training_days, onboarding_equipment_items,
         onboarding_nutrition_preference, onboarding_nutrition_restrictions, first_run_completed)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final RowMapper<UserProfile> ROW_MAPPER =
      (rs, rowNum) ->
          new UserProfile(
              rs.getString("owner_id"),
              rs.getString("name"),
              rs.getString("email"),
              rs.getObject("birth_date", LocalDate.class),
              toEnum(Sex.class, rs.getString("sex")),
              toNullableDouble(rs.getBigDecimal("height_cm")),
              toEnum(ActivityLevel.class, rs.getString("activity_level")),
              toEnum(MainGoal.class, rs.getString("main_goal")),
              new UnitPreferences(
                  WeightUnit.valueOf(rs.getString("weight_unit")),
                  HeightUnit.valueOf(rs.getString("height_unit")),
                  DistanceUnit.valueOf(rs.getString("distance_unit")),
                  EnergyUnit.valueOf(rs.getString("energy_unit"))),
              new DefaultObjectives(
                  toNullableDouble(rs.getBigDecimal("caloric_deficit_kcal")),
                  toNullableDouble(rs.getBigDecimal("protein_target_g")),
                  toNullableDouble(rs.getBigDecimal("daily_water_ml"))),
              ThemeMode.valueOf(rs.getString("theme_mode")),
              new OnboardingAnswers(
                  new OnboardingAnswers.ProfileDraft(
                      rs.getString("onboarding_profile_name"),
                      rs.getString("onboarding_profile_birth_date"),
                      rs.getString("onboarding_profile_sex"),
                      rs.getString("onboarding_profile_height_cm")),
                  new OnboardingAnswers.MetricsDraft(
                      rs.getString("onboarding_metrics_choice"),
                      rs.getBoolean("onboarding_metrics_saved")),
                  new OnboardingAnswers.GoalDraft(rs.getString("onboarding_goal_selected")),
                  new OnboardingAnswers.TrainingDraft(
                      splitCsv(rs.getString("onboarding_training_days"))),
                  new OnboardingAnswers.EquipmentDraft(
                      splitCsv(rs.getString("onboarding_equipment_items"))),
                  new OnboardingAnswers.NutritionDraft(
                      rs.getString("onboarding_nutrition_preference"),
                      rs.getString("onboarding_nutrition_restrictions"))),
              rs.getBoolean("first_run_completed"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserProfileRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<UserProfile> find(String ownerId) {
    List<UserProfile> found = jdbcTemplate.query(FIND_SQL, ROW_MAPPER, ownerId);
    return found.stream().findFirst();
  }

  @Override
  public void save(UserProfile profile) {
    Object[] fields = rowValues(profile);
    Object[] updateArgs = Arrays.copyOf(fields, fields.length + 1);
    updateArgs[fields.length] = profile.ownerId();
    int updated = jdbcTemplate.update(UPDATE_SQL, updateArgs);
    if (updated == 0) {
      Object[] insertArgs = new Object[fields.length + 1];
      insertArgs[0] = profile.ownerId();
      System.arraycopy(fields, 0, insertArgs, 1, fields.length);
      jdbcTemplate.update(INSERT_SQL, insertArgs);
    }
  }

  /** Column values shared by UPDATE and INSERT, in the order the table declares them (sans id). */
  private static Object[] rowValues(UserProfile profile) {
    UnitPreferences units = profile.unitPreferences();
    DefaultObjectives objectives = profile.defaultObjectives();
    OnboardingAnswers onboarding = profile.onboardingAnswers();
    return new Object[] {
      profile.name(),
      profile.email(),
      profile.birthDate() == null ? null : Date.valueOf(profile.birthDate()),
      profile.sex() == null ? null : profile.sex().name(),
      toNullableBigDecimal(profile.heightCm()),
      profile.activityLevel() == null ? null : profile.activityLevel().name(),
      profile.mainGoal() == null ? null : profile.mainGoal().name(),
      units.weightUnit().name(),
      units.heightUnit().name(),
      units.distanceUnit().name(),
      units.energyUnit().name(),
      toNullableBigDecimal(objectives.caloricDeficitKcal()),
      toNullableBigDecimal(objectives.proteinTargetG()),
      toNullableBigDecimal(objectives.dailyWaterMl()),
      profile.themeMode().name(),
      onboarding.profile().name(),
      onboarding.profile().birthDate(),
      onboarding.profile().sex(),
      onboarding.profile().heightCm(),
      onboarding.metrics().choice(),
      onboarding.metrics().measurementSaved(),
      onboarding.goal().selected(),
      String.join(",", onboarding.training().days()),
      String.join(",", onboarding.equipment().items()),
      onboarding.nutrition().preference(),
      onboarding.nutrition().restrictions(),
      profile.firstRunCompleted()
    };
  }

  private static List<String> splitCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(",")).toList();
  }

  private static <E extends Enum<E>> E toEnum(Class<E> type, String raw) {
    return raw == null ? null : Enum.valueOf(type, raw);
  }

  private static Double toNullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private static BigDecimal toNullableBigDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }
}

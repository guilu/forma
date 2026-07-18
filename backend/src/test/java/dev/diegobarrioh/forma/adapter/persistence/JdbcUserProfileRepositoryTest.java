package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.UserProfileRepository;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.ProfileBaseline;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
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
 * Integration test for {@link JdbcUserProfileRepository} (FOR-107). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like the FOR-16/FOR-27 tests. Covers
 * the "clean-database → defaults" and "seeded row → round-trip" fixtures from tests.md.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcUserProfileRepositoryTest {

  @Autowired private UserProfileRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM user_profile");
  }

  @Test
  void findReturnsEmptyOnACleanDatabase() {
    assertThat(repository.find("default-user")).isEmpty();
  }

  @Test
  void savesAndReadsBackAFullProfile() {
    UserProfile profile =
        new UserProfile(
            "default-user",
            "Ada Lovelace",
            "ada@example.com",
            LocalDate.of(1990, 1, 1),
            Sex.FEMALE,
            170.5,
            ActivityLevel.MODERATE,
            MainGoal.COMPOSICION,
            UnitPreferences.DEFAULT,
            new DefaultObjectives(500.0, 140.0, 2500.0),
            ThemeMode.LIGHT,
            new OnboardingAnswers(
                new OnboardingAnswers.ProfileDraft("Ada", "1990-01-01", "FEMALE", "170"),
                new OnboardingAnswers.MetricsDraft("MANUAL", true),
                new OnboardingAnswers.GoalDraft("COMPOSICION"),
                new OnboardingAnswers.TrainingDraft(List.of("MONDAY", "THURSDAY")),
                new OnboardingAnswers.EquipmentDraft(List.of("DUMBBELLS", "MAT")),
                new OnboardingAnswers.NutritionDraft("high-protein", "lactose")),
            true,
            new ProfileBaseline(73.6, 14.7, 22.7),
            new PersonalTargets(2300.0, 12.0, 13.0, 73.0, 75.0, 70.0, 260.0));

    repository.save(profile);
    Optional<UserProfile> read = repository.find("default-user");

    assertThat(read).isPresent();
    UserProfile stored = read.orElseThrow();
    assertThat(stored.name()).isEqualTo("Ada Lovelace");
    assertThat(stored.email()).isEqualTo("ada@example.com");
    assertThat(stored.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    assertThat(stored.sex()).isEqualTo(Sex.FEMALE);
    assertThat(stored.heightCm()).isEqualTo(170.5);
    assertThat(stored.activityLevel()).isEqualTo(ActivityLevel.MODERATE);
    assertThat(stored.mainGoal()).isEqualTo(MainGoal.COMPOSICION);
    assertThat(stored.unitPreferences()).isEqualTo(UnitPreferences.DEFAULT);
    assertThat(stored.defaultObjectives().caloricDeficitKcal()).isEqualTo(500.0);
    assertThat(stored.themeMode()).isEqualTo(ThemeMode.LIGHT);
    assertThat(stored.onboardingAnswers().profile().name()).isEqualTo("Ada");
    assertThat(stored.onboardingAnswers().training().days()).containsExactly("MONDAY", "THURSDAY");
    assertThat(stored.onboardingAnswers().equipment().items()).containsExactly("DUMBBELLS", "MAT");
    assertThat(stored.firstRunCompleted()).isTrue();
    assertThat(stored.profileBaseline().weightKg()).isEqualTo(73.6);
    assertThat(stored.profileBaseline().bodyFatPct()).isEqualTo(14.7);
    assertThat(stored.profileBaseline().bmi()).isEqualTo(22.7);
    assertThat(stored.personalTargets().baseCaloriesKcal()).isEqualTo(2300.0);
    assertThat(stored.personalTargets().bodyFatTargetMinPct()).isEqualTo(12.0);
    assertThat(stored.personalTargets().bodyFatTargetMaxPct()).isEqualTo(13.0);
    assertThat(stored.personalTargets().weightTargetMinKg()).isEqualTo(73.0);
    assertThat(stored.personalTargets().weightTargetMaxKg()).isEqualTo(75.0);
    assertThat(stored.personalTargets().fatTargetG()).isEqualTo(70.0);
    assertThat(stored.personalTargets().carbsTargetG()).isEqualTo(260.0);
  }

  @Test
  void saveUpsertsAnExistingRowInPlace() {
    repository.save(UserProfile.defaults("default-user"));
    UserProfile updated =
        new UserProfile(
            "default-user",
            "Renamed",
            null,
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            DefaultObjectives.EMPTY,
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY);

    repository.save(updated);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_profile", Integer.class))
        .isEqualTo(1);
    assertThat(repository.find("default-user").orElseThrow().name()).isEqualTo("Renamed");
  }

  @Test
  void roundTripsNullableFieldsAndEmptyOnboardingLists() {
    repository.save(UserProfile.defaults("default-user"));

    UserProfile stored = repository.find("default-user").orElseThrow();
    assertThat(stored.name()).isNull();
    assertThat(stored.birthDate()).isNull();
    assertThat(stored.sex()).isNull();
    assertThat(stored.defaultObjectives().caloricDeficitKcal()).isNull();
    assertThat(stored.profileBaseline()).isEqualTo(ProfileBaseline.EMPTY);
    assertThat(stored.personalTargets()).isEqualTo(PersonalTargets.EMPTY);
    assertThat(stored.onboardingAnswers().training().days()).isEmpty();
    assertThat(stored.onboardingAnswers().equipment().items()).isEmpty();
  }

  @Test
  void roundTripsPartialBaselineAndTargets() {
    UserProfile withPartialTargets =
        new UserProfile(
            "default-user",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            DefaultObjectives.EMPTY,
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            new ProfileBaseline(73.6, null, null),
            new PersonalTargets(2300.0, null, null, null, null, null, null));

    repository.save(withPartialTargets);
    UserProfile stored = repository.find("default-user").orElseThrow();

    assertThat(stored.profileBaseline().weightKg()).isEqualTo(73.6);
    assertThat(stored.profileBaseline().bodyFatPct()).isNull();
    assertThat(stored.profileBaseline().bmi()).isNull();
    assertThat(stored.personalTargets().baseCaloriesKcal()).isEqualTo(2300.0);
    assertThat(stored.personalTargets().bodyFatTargetMinPct()).isNull();
  }
}

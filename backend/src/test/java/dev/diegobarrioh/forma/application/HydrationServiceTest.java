package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.UserProfile;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link HydrationService} (FOR-130, hydration slice of FOR-102):
 * logging water intake (owner-scoped, ADR-002), input validation, and the hydration progress read
 * model resolving the daily goal from the FOR-107 profile with a documented fallback. Hand-rolled
 * in-memory fakes (no Spring, no Mockito), matching {@code MealLogServiceTest} (FOR-127).
 */
class HydrationServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

  private final RecordingWaterIntakeRepository repository = new RecordingWaterIntakeRepository();
  private final RecordingUserProfileRepository profileRepository =
      new RecordingUserProfileRepository();
  private final UserProfileService userProfileService = new UserProfileService(profileRepository);
  private final HydrationService service =
      new HydrationService(repository, userProfileService, FIXED_CLOCK);

  @Test
  void logsAPositiveVolumeForTheOwner() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(TODAY, 500.0);

    StoredWaterIntakeEntry stored = service.log(command);

    assertThat(stored.id()).isNotBlank();
    assertThat(stored.entry().date()).isEqualTo(TODAY);
    assertThat(stored.entry().volumeMl()).isEqualTo(500.0);
    assertThat(repository.rows).hasSize(1);
    assertThat(repository.rows.get(0).ownerId).isEqualTo(HydrationService.OWNER_ID);
  }

  @Test
  void rejectsAZeroVolume() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(TODAY, 0.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsANegativeVolume() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(TODAY, -50.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAMissingVolume() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(TODAY, null);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAFarFutureDate() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(TODAY.plusYears(1), 500.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void rejectsAMissingDate() {
    LogWaterIntakeCommand command = new LogWaterIntakeCommand(null, 500.0);

    assertThatThrownBy(() -> service.log(command)).isInstanceOf(ValidationException.class);
  }

  @Test
  void hydrationProgressForADayWithNoEntriesReturnsZeroedTotalNeverAnError() {
    HydrationProgress progress = service.hydrationProgress(TODAY);

    assertThat(progress.totalMl()).isZero();
    assertThat(progress.entries()).isEmpty();
  }

  @Test
  void hydrationProgressSumsMultipleEntriesForTheSameDay() {
    service.log(new LogWaterIntakeCommand(TODAY, 500.0));
    service.log(new LogWaterIntakeCommand(TODAY, 300.0));

    HydrationProgress progress = service.hydrationProgress(TODAY);

    assertThat(progress.entries()).hasSize(2);
    assertThat(progress.totalMl()).isEqualTo(800.0);
  }

  @Test
  void hydrationProgressReadsTheGoalFromTheProfilesDefaultObjectives() {
    profileRepository.rows.put(UserProfileService.OWNER_ID, profileWithDailyWaterMl(2500.0));
    service.log(new LogWaterIntakeCommand(TODAY, 1250.0));

    HydrationProgress progress = service.hydrationProgress(TODAY);

    assertThat(progress.goalMl()).isEqualTo(2500.0);
    assertThat(progress.progress()).isEqualTo(0.5);
  }

  @Test
  void hydrationProgressAppliesTheFallbackDefaultWhenDailyWaterMlIsUnset() {
    // No profile row saved -> UserProfileService.get() returns UserProfile.defaults(), whose
    // defaultObjectives().dailyWaterMl() is null (spec FOR-130 Open Questions: documented
    // fallback default applies).
    HydrationProgress progress = service.hydrationProgress(TODAY);

    assertThat(progress.goalMl()).isEqualTo(HydrationService.DEFAULT_DAILY_WATER_ML_FALLBACK);
  }

  @Test
  void hydrationProgressOnlyReflectsTheOwnersEntries() {
    repository.rows.add(
        new OwnedEntry(
            "other-user",
            new StoredWaterIntakeEntry(
                UUID.randomUUID().toString(), new WaterIntakeEntry(TODAY, 9999.0))));

    HydrationProgress progress = service.hydrationProgress(TODAY);

    assertThat(progress.entries()).isEmpty();
    assertThat(progress.totalMl()).isZero();
  }

  private static UserProfile profileWithDailyWaterMl(double dailyWaterMl) {
    return new UserProfile(
        UserProfileService.OWNER_ID,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        new DefaultObjectives(null, null, dailyWaterMl),
        null,
        null,
        false,
        null,
        null);
  }

  /** In-memory fake, matching {@code RecordingMealLogRepository} (FOR-127). */
  private static class RecordingWaterIntakeRepository implements WaterIntakeRepository {
    final List<OwnedEntry> rows = new ArrayList<>();

    @Override
    public List<StoredWaterIntakeEntry> findByOwnerAndDate(String ownerId, LocalDate date) {
      return rows.stream()
          .filter(r -> r.ownerId.equals(ownerId) && r.stored.entry().date().equals(date))
          .map(r -> r.stored)
          .toList();
    }

    @Override
    public StoredWaterIntakeEntry save(String ownerId, WaterIntakeEntry entry) {
      StoredWaterIntakeEntry stored =
          new StoredWaterIntakeEntry(UUID.randomUUID().toString(), entry);
      rows.add(new OwnedEntry(ownerId, stored));
      return stored;
    }
  }

  private record OwnedEntry(String ownerId, StoredWaterIntakeEntry stored) {}

  /** In-memory fake, matching {@code UserProfileServiceTest}'s {@code RecordingRepository}. */
  private static class RecordingUserProfileRepository implements UserProfileRepository {
    final Map<String, UserProfile> rows = new HashMap<>();

    @Override
    public Optional<UserProfile> find(String ownerId) {
      return Optional.ofNullable(rows.get(ownerId));
    }

    @Override
    public void save(UserProfile profile) {
      rows.put(profile.ownerId(), profile);
    }
  }
}

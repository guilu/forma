package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.HydrationLog;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Application use cases for water-intake logging and the hydration progress read model (FOR-130,
 * hydration slice of FOR-102). Independent of meal logging (FOR-127) and consumed-vs-target
 * (FOR-128) — hydration has its own aggregate and its own goal source.
 *
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row, mirroring
 * {@link MealLogService#OWNER_ID}/{@link UserProfileService#OWNER_ID}. Never logs intake volumes at
 * INFO (personal health data, AGENTS.md) — only the outcome (entry id) may be logged by callers if
 * needed.
 *
 * <p><b>Daily goal resolution (spec FOR-130 Open Questions).</b> {@link #hydrationProgress}
 * resolves the goal from the FOR-107 profile's {@code DefaultObjectives.dailyWaterMl} via {@link
 * UserProfileService#get()} — reusing the existing profile service rather than duplicating
 * profile/repository logic (the hydration service depends on the profile service, never the
 * reverse, so there is no circular dependency). When {@code dailyWaterMl} is unset ({@code null}),
 * the documented fallback {@link #DEFAULT_DAILY_WATER_ML_FALLBACK} is used instead of leaving
 * progress unfabricated-but-undefined. In practice this means {@link HydrationLog#progressToward}
 * is never asked to return {@code null} through this service — a goal is always resolvable — but
 * the null-safe contract is kept end-to-end (domain and read model) in case a future story removes
 * the fallback.
 */
@Service
public class HydrationService {

  /**
   * Fixed single-user owner id for the MVP (ADR-002), mirroring {@link MealLogService#OWNER_ID}.
   */
  public static final String OWNER_ID = "default-user";

  /**
   * Documented fallback daily water goal (ml) used when the profile's {@code
   * DefaultObjectives.dailyWaterMl} is unset (spec FOR-130 Open Questions: "pick a documented
   * sensible value (e.g. 2000 ml)"). 2000 ml/day is a commonly cited general hydration guideline
   * and matches the example value used throughout {@code specs/FOR-130/api.md}.
   */
  public static final double DEFAULT_DAILY_WATER_ML_FALLBACK = 2000.0;

  private final WaterIntakeRepository repository;
  private final UserProfileService userProfileService;
  private final Clock clock;

  public HydrationService(
      WaterIntakeRepository repository, UserProfileService userProfileService, Clock clock) {
    this.repository = repository;
    this.userProfileService = userProfileService;
    this.clock = clock;
  }

  /**
   * Logs a water-intake volume for the owner. Never logs {@code command} contents (personal health
   * data, AGENTS.md) — only the outcome (entry id) may be logged by callers if needed.
   *
   * @throws ValidationException if the date is missing/far in the future, or {@code volumeMl} is
   *     missing or not strictly positive
   */
  public StoredWaterIntakeEntry log(LogWaterIntakeCommand command) {
    validateDate(command.date());
    if (command.volumeMl() == null || command.volumeMl() <= 0) {
      throw new ValidationException("volumeMl must be strictly positive");
    }
    WaterIntakeEntry entry = new WaterIntakeEntry(command.date(), command.volumeMl());
    return repository.save(OWNER_ID, entry);
  }

  /**
   * The owner's hydration progress read model for {@code date}: total logged volume derived fresh
   * from that day's entries, plus the resolved daily goal and progress. Never 404s — an empty day
   * returns a zeroed total (spec FOR-130 edge case), with the goal/progress still resolved.
   *
   * @throws ValidationException if {@code date} is missing or far in the future
   */
  public HydrationProgress hydrationProgress(LocalDate date) {
    validateDate(date);
    var stored = repository.findByOwnerAndDate(OWNER_ID, date);
    HydrationLog log =
        stored.stream()
            .map(StoredWaterIntakeEntry::entry)
            .reduce(HydrationLog.empty(date), HydrationLog::withEntry, (a, b) -> b);

    Double goalMl = resolveGoalMl();
    Double progress = log.progressToward(goalMl);

    return new HydrationProgress(date, log.totalMl(), goalMl, progress, stored);
  }

  /**
   * Resolves the daily water goal from the profile's {@code DefaultObjectives.dailyWaterMl},
   * falling back to {@link #DEFAULT_DAILY_WATER_ML_FALLBACK} when unset (spec FOR-130 Open
   * Questions). Reuses {@link UserProfileService#get()} — no duplicated profile logic.
   */
  private Double resolveGoalMl() {
    Double configured = userProfileService.get().defaultObjectives().dailyWaterMl();
    return configured != null ? configured : DEFAULT_DAILY_WATER_ML_FALLBACK;
  }

  private void validateDate(LocalDate date) {
    if (date == null) {
      throw new ValidationException("date is required");
    }
    LocalDate maxAllowed = LocalDate.now(clock).plusDays(1);
    if (date.isAfter(maxAllowed)) {
      throw new ValidationException("date must not be in the far future");
    }
  }
}

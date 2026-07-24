package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.AdherenceCategory;
import dev.diegobarrioh.forma.domain.CategoryAdherence;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application use case for the adherence read model (FOR-129, second implementable slice of
 * FOR-104): planned vs completed per category over a rolling window ending today, derived entirely
 * from existing repositories/services — no new persistence, no domain aggregate to persist (spec
 * FOR-129 Data Model Notes).
 *
 * <p><b>TRAINING</b> reuses {@link WeeklyTrainingScheduleService#currentWeek()} — which itself
 * reuses the shared {@link dev.diegobarrioh.forma.domain.WeeklyTrainingDayPolicy} (FOR-128) and
 * applies stored {@link TrainingSessionStatusRepository} statuses — instead of re-deriving the
 * weekday/session policy (spec FOR-129: "no duplicated counting/policy"). <b>Documented MVP
 * limitation</b>: {@code training_session_status} (FOR-27 migration V3) stores one current status
 * per weekday session slot (e.g. {@code "SATURDAY:RUNNING"}), not a timestamped per-date history —
 * there is no stored fact about what a session's status was on a *specific past Saturday*, only
 * what it is *right now*. This service therefore projects the current week's per-weekday
 * planned/completed pattern uniformly across every occurrence of that weekday inside the window.
 * This is the most honest derivation the existing repository shape supports without adding
 * persistence (forbidden by this story's NFR) — a real per-date completion history is a candidate
 * for a later FOR-104 slice.
 *
 * <p><b>NUTRITION</b> reuses {@link MealLogRepository#findByOwnerAndDate}: {@code completed} is the
 * count of window days with at least one logged entry; {@code planned} is the window length itself
 * (spec FOR-129 Open Questions: no per-day "planned meals" count exists today, so "planned" = "days
 * in window" is the documented MVP definition — a "logged consistently" measure, not a
 * plan-adherence measure). No by-date-range query exists on {@link MealLogRepository}, so this
 * loops the existing per-date query across the window (bounded to &le;365 days by {@link
 * #MAX_DAYS}) rather than adding a new port method — the smallest honest option, acceptable at MVP
 * volume (spec FOR-129 NFR "Performance").
 *
 * <p><b>MEASUREMENTS</b> reuses {@link BodyMeasurementRepository#list()}, filtering to the window
 * by {@code measuredAt}; {@code planned} is the expected count under an assumed <b>weekly</b>
 * cadence ({@code ceil(days / 7.0)}). The cadence is not derived from any stored preference (none
 * exists today) — it is a documented MVP assumption (spec FOR-129 Open Questions: "expected cadence
 * — weekly by default?"), not a fabricated fact.
 *
 * <p><b>Owner-scoping (ADR-002, spec FOR-129 "mirror the FOR-127/128 owner scoping").</b> Real
 * multi-user auth (FOR-145b-2, ADR-012): NUTRITION resolves the caller's account id via {@link
 * CurrentUserProvider} and passes it to {@link MealLogRepository} on every call — replacing the old
 * fixed {@code OWNER_ID = "default-user"} constant and the 145b-1 interim {@code
 * requireLegacyOwner()} guard (both removed by this slice). <b>Documented discrepancy vs the spec's
 * owner-scoping expectation, carried over from 145b-1</b>: {@link BodyMeasurementRepository}
 * (FOR-16, migration V2) and {@link TrainingSessionStatusRepository}/{@link
 * WeeklyTrainingScheduleService} (FOR-26/27, migration V3) predate the owner-scoping convention
 * introduced by FOR-125/127 — their tables have no {@code user_id} column and their ports take no
 * owner parameter at all, so nothing exists to filter on. TRAINING and MEASUREMENTS therefore
 * cannot be scoped to a real per-user boundary yet.
 *
 * <p><b>INTERIM security guard (post-145b-2 security review, 🟠 MEDIUM cross-account signal
 * leak).</b> Reading the unscoped tables for a real, non-placeholder caller would leak every
 * account's global training/measurement activity into that caller's adherence numbers. Until 145c
 * adds {@code user_id} to {@code body_measurements}/{@code training_session_status}, {@link
 * #compute(int)} only computes TRAINING/MEASUREMENTS from those tables for the seeded legacy
 * placeholder account ({@link LegacyUserBootstrap#PLACEHOLDER_USER_ID}) — every other caller gets
 * both categories back as {@code planned=0, completed=0} (never a 404, "empty is fine" per spec
 * FOR-129) and the global repositories are not even consulted for that caller. NUTRITION is
 * unaffected — {@link MealLogRepository} is properly {@code user_id}-scoped (145b-1) and is always
 * computed for real. <b>Remove this guard in 145c</b> once both tables carry {@code user_id} and
 * this service can scope TRAINING/MEASUREMENTS like NUTRITION already is.
 */
@Service
public class AdherenceService {

  /**
   * Bounded {@code days} range (spec FOR-129 Edge Cases): outside this, the request is rejected.
   */
  static final int MIN_DAYS = 1;

  static final int MAX_DAYS = 365;

  private final WeeklyTrainingScheduleService scheduleService;
  private final MealLogRepository mealLogRepository;
  private final BodyMeasurementRepository bodyMeasurementRepository;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public AdherenceService(
      WeeklyTrainingScheduleService scheduleService,
      MealLogRepository mealLogRepository,
      BodyMeasurementRepository bodyMeasurementRepository,
      Clock clock,
      CurrentUserProvider currentUserProvider) {
    this.scheduleService = scheduleService;
    this.mealLogRepository = mealLogRepository;
    this.bodyMeasurementRepository = bodyMeasurementRepository;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
  }

  /**
   * Computes the adherence read model for a {@code days}-long window ending today (inclusive on
   * both ends: {@code [today - days + 1, today]}).
   *
   * @throws ValidationException if {@code days} is outside {@code [1, 365]}
   */
  public Adherence compute(int days) {
    if (days < MIN_DAYS || days > MAX_DAYS) {
      throw new ValidationException(
          "days must be between " + MIN_DAYS + " and " + MAX_DAYS + ", was: " + days);
    }
    UUID userId = currentUserProvider.currentUserId();
    boolean isLegacyPlaceholder = LegacyUserBootstrap.PLACEHOLDER_USER_ID.equals(userId);

    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(days - 1L);

    // INTERIM security guard (145c gap, see class javadoc): only the seeded legacy placeholder
    // account reads the still-unscoped global training/measurement tables; every other caller gets
    // those two categories back zeroed, without consulting the global repositories at all.
    List<CategoryAdherence> categories =
        List.of(
            isLegacyPlaceholder
                ? training(from, to)
                : CategoryAdherence.of(AdherenceCategory.TRAINING, 0, 0),
            nutrition(userId, from, to, days),
            isLegacyPlaceholder
                ? measurements(from, to, days)
                : CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, 0, 0));
    return new Adherence(days, from, to, categories);
  }

  private CategoryAdherence training(LocalDate from, LocalDate to) {
    Map<DayOfWeek, Integer> plannedByWeekday = new EnumMap<>(DayOfWeek.class);
    Map<DayOfWeek, Integer> completedByWeekday = new EnumMap<>(DayOfWeek.class);
    for (TrainingDay day : scheduleService.currentWeek().days()) {
      plannedByWeekday.put(day.dayOfWeek(), day.entries().size());
      long completed =
          day.entries().stream()
              .filter(entry -> SessionStatus.COMPLETED.name().equals(entry.status()))
              .count();
      completedByWeekday.put(day.dayOfWeek(), (int) completed);
    }

    int planned = 0;
    int completed = 0;
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      planned += plannedByWeekday.getOrDefault(date.getDayOfWeek(), 0);
      completed += completedByWeekday.getOrDefault(date.getDayOfWeek(), 0);
    }
    return CategoryAdherence.of(AdherenceCategory.TRAINING, planned, completed);
  }

  private CategoryAdherence nutrition(UUID userId, LocalDate from, LocalDate to, int windowDays) {
    int completed = 0;
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      if (!mealLogRepository.findByOwnerAndDate(userId, date).isEmpty()) {
        completed++;
      }
    }
    return CategoryAdherence.of(AdherenceCategory.NUTRITION, windowDays, completed);
  }

  private CategoryAdherence measurements(LocalDate from, LocalDate to, int windowDays) {
    int planned = (int) Math.ceil(windowDays / 7.0);
    long completed =
        bodyMeasurementRepository.list().stream()
            .map(measurement -> LocalDate.ofInstant(measurement.measuredAt(), clock.getZone()))
            .filter(date -> !date.isBefore(from) && !date.isAfter(to))
            .count();
    return CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, planned, (int) completed);
  }
}

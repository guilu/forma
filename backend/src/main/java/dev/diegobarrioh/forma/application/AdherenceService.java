package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
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
 * multi-user auth (FOR-145, ADR-012): every category resolves the caller's account id via {@link
 * CurrentUserProvider} and computes exclusively from that caller's own rows. NUTRITION reads {@link
 * MealLogRepository} (scoped since 145b-1). TRAINING reads {@link
 * WeeklyTrainingScheduleService#currentWeek()}, which resolves the caller via its own injected
 * {@link CurrentUserProvider} and reads {@link TrainingSessionStatusRepository} — scoped since 145c
 * migration V31, which rebuilt {@code training_session_status}'s primary key from a bare {@code
 * session_id} (colliding across accounts, e.g. every user's Saturday run shared one row) to {@code
 * (user_id, session_id)}. MEASUREMENTS reads {@link BodyMeasurementRepository} — scoped since 145c
 * migration V30, which added a plain {@code user_id} column (with a backfill) to {@code
 * body_measurements}.
 *
 * <p><b>145c removed the 145b-2 INTERIM security guard</b> that computed TRAINING/MEASUREMENTS only
 * for the seeded legacy placeholder account and zeroed those two categories for every other caller
 * (🟠 MEDIUM cross-account signal leak, since neither table carried {@code user_id} yet at that
 * point). Both tables are now properly scoped, so every caller — placeholder or not — gets real,
 * isolated TRAINING/MEASUREMENTS numbers derived only from their own data.
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

    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(days - 1L);

    List<CategoryAdherence> categories =
        List.of(
            training(from, to),
            nutrition(userId, from, to, days),
            measurements(userId, from, to, days));
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

  private CategoryAdherence measurements(
      UUID userId, LocalDate from, LocalDate to, int windowDays) {
    int planned = (int) Math.ceil(windowDays / 7.0);
    long completed =
        bodyMeasurementRepository.list(userId).stream()
            .map(measurement -> LocalDate.ofInstant(measurement.measuredAt(), clock.getZone()))
            .filter(date -> !date.isBefore(from) && !date.isAfter(to))
            .count();
    return CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, planned, (int) completed);
  }
}

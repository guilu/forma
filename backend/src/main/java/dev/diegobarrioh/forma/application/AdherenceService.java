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
 * <p><b>Owner-scoping (ADR-002, spec FOR-129 "mirror the FOR-127/128 owner scoping"):</b> NUTRITION
 * is owner-scoped exactly like FOR-127 — {@link MealLogRepository} takes {@code ownerId} on every
 * call. <b>Documented discrepancy vs the spec's owner-scoping expectation</b>: {@link
 * BodyMeasurementRepository} (FOR-16, migration V2) and {@link TrainingSessionStatusRepository}/
 * {@link WeeklyTrainingScheduleService} (FOR-26/27, migration V3) predate the owner-scoping
 * convention introduced by FOR-125/127 — their tables have no {@code owner_id} column and their
 * ports take no owner parameter at all, so nothing exists to filter on. TRAINING and MEASUREMENTS
 * therefore cannot be scoped to an owner today; this is harmless in the current single-user MVP
 * (there is exactly one owner) but is not a real per-owner boundary the way NUTRITION's is. Adding
 * {@code owner_id} to those tables is a migration and is explicitly out of scope for this
 * pure-derivation story (spec FOR-129 NFR "No new persistence/migration") — flagged here for a
 * future story rather than invented or silently ignored (AGENTS.md: "repository state has priority
 * over docs; document the discrepancy").
 */
@Service
public class AdherenceService {

  /**
   * Fixed single-user owner id for the MVP (ADR-002), mirroring {@link MealLogService#OWNER_ID} /
   * {@link GoalService#OWNER_ID}. Duplicated here rather than introduced as a shared abstraction —
   * see {@link GoalService}'s javadoc for the rationale (no speculative abstraction beyond scope).
   */
  public static final String OWNER_ID = "default-user";

  /**
   * FOR-145b-1 compile-compat shim: {@link MealLogRepository} (Class A, migration V27) now takes a
   * real {@code UUID}. {@code AdherenceService} itself stays on the legacy String {@link #OWNER_ID}
   * for now (deferred to 145b-2 — it also reuses {@link BodyMeasurementRepository}, a gap table not
   * scoped at all until 145c) — this constant is ONLY the UUID equivalent of that same legacy
   * owner, used solely for the {@link #mealLogRepository} call below. Not a behavior change: {@code
   * OWNER_ID = "default-user"} and this UUID resolve to the identical legacy account.
   */
  private static final UUID LEGACY_OWNER_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

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
   * @throws NotFoundException if the caller is not the legacy placeholder account (interim security
   *     guard, mandatory review of 145b-1, HIGH cross-account disclosure — see {@link
   *     #requireLegacyOwner()})
   */
  public Adherence compute(int days) {
    requireLegacyOwner();
    if (days < MIN_DAYS || days > MAX_DAYS) {
      throw new ValidationException(
          "days must be between " + MIN_DAYS + " and " + MAX_DAYS + ", was: " + days);
    }

    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(days - 1L);

    List<CategoryAdherence> categories =
        List.of(training(from, to), nutrition(from, to, days), measurements(from, to, days));
    return new Adherence(days, from, to, categories);
  }

  /**
   * Interim security guard (mandatory review of 145b-1, HIGH cross-account disclosure): this
   * service's TRAINING/MEASUREMENTS categories are not owner-scoped at all yet (documented above)
   * and its NUTRITION category still reads only the legacy placeholder owner ({@link
   * #LEGACY_OWNER_UUID}). Until 145b-2 wires a real per-user owner here, any authenticated caller
   * other than the placeholder account must get a 404, never the legacy owner's adherence data.
   *
   * @throws NotFoundException if the caller is not the legacy placeholder account
   */
  private void requireLegacyOwner() {
    if (!currentUserProvider.currentUserId().equals(LEGACY_OWNER_UUID)) {
      throw new NotFoundException("No existen datos de progreso para este usuario");
    }
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

  private CategoryAdherence nutrition(LocalDate from, LocalDate to, int windowDays) {
    int completed = 0;
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      if (!mealLogRepository.findByOwnerAndDate(LEGACY_OWNER_UUID, date).isEmpty()) {
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

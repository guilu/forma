package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionDayTypeResolver;
import dev.diegobarrioh.forma.domain.WeeklyTrainingDayPolicy;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;

/**
 * Resolves a date's {@link NutritionDayType} from the training the user actually has planned that
 * day, rather than from the weekday alone.
 *
 * <p>Sessions can be moved within a week (V60), so "Monday is a running day" stopped being true by
 * construction: doing Tuesday's push on Monday makes Monday a strength day, and the day's
 * consumption target should follow the training, not the calendar column. Without this the two
 * views disagree silently — the calendar shows a lift while the nutrition screen still budgets for
 * a run.
 *
 * <p>Layered rather than folded into {@link NutritionDayTypeResolver}: that resolver is pure and
 * framework-free (ADR-001) and reads no persistence, which is exactly why it cannot know about a
 * stored override. It stays as the fallback, so the shared {@link WeeklyTrainingDayPolicy} is still
 * the single source of the day-kind policy and this class only layers the overrides on top — no
 * duplicated classification.
 *
 * <p><b>Only the current week carries overrides.</b> The calendar composes one week (the current
 * one), so that is the only week whose real placement is known. Any other date falls back to the
 * policy rather than having this week's moves projected onto it, which would invent a schedule for
 * a week nobody arranged.
 */
@Service
public class ScheduledNutritionDayTypeService {

  private static final String RUNNING_KIND = "RUNNING";
  private static final String STRENGTH_KIND = "STRENGTH";

  private final WeeklyTrainingScheduleService scheduleService;
  private final Clock clock;

  public ScheduledNutritionDayTypeService(
      WeeklyTrainingScheduleService scheduleService, Clock clock) {
    this.scheduleService = scheduleService;
    this.clock = clock;
  }

  /** The day type for {@code date}, honouring any session moved this week. */
  public NutritionDayType resolve(LocalDate date) {
    if (!isCurrentWeek(date)) {
      return NutritionDayTypeResolver.resolve(date);
    }

    for (TrainingDay day : scheduleService.currentWeek().days()) {
      if (day.dayOfWeek() != date.getDayOfWeek()) {
        continue;
      }
      boolean running = day.entries().stream().anyMatch(entry -> isKind(entry, RUNNING_KIND));
      if (running) {
        // Running wins over strength on a day holding both — the precedence
        // WeeklyTrainingDayPolicy documents, applied here where it is finally reachable.
        return NutritionDayType.RUNNING;
      }
      boolean strength = day.entries().stream().anyMatch(entry -> isKind(entry, STRENGTH_KIND));
      return strength ? NutritionDayType.STRENGTH : NutritionDayType.REST;
    }
    return NutritionDayTypeResolver.resolve(date);
  }

  private static boolean isKind(TrainingEntry entry, String kind) {
    return kind.equals(entry.kind());
  }

  private boolean isCurrentWeek(LocalDate date) {
    LocalDate monday =
        LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return !date.isBefore(monday) && !date.isAfter(monday.plusDays(6));
  }
}

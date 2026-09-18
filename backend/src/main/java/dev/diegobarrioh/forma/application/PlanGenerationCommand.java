package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Everything {@link PlanGenerationGateway#generate} sends the plan agent — the outbound half of
 * {@code docs/FORMA_Contrato_Agente_Plan.md} ("La petición"), as FORMA's own types (ADR-004: no
 * provider JSON, no HTTP type crosses this boundary). Grouped the way the contract document groups
 * it ({@code person}/{@code goal}/{@code targets}/{@code training}/{@code preferences}/{@code
 * plan}) rather than as one flat record, mirroring how {@code delivery/plan/NutritionPlanRequest}
 * nests {@code Targets}/{@code Generation}/{@code Day}/{@code Meal}/{@code Item}.
 *
 * <p><b>Disagreement with {@code application/PlanRequest} (ADR-015 slices 1-4), called out rather
 * than papered over.</b> {@link PlanScope#weeks} and {@link PlanScope#startDate} are required by
 * the contract's {@code plan.weeks}/{@code plan.startDate}, but the {@code plan_request} table
 * (V63) has no columns for either — its schema stops at what a human asked for (targets, training
 * days, preferences), never how many weeks or which calendar week to start. Slice 6 (the dispatcher
 * that will actually build one of these) has to decide where those two values come from: a
 * hardcoded "1 week starting next Monday", a new question the wizard asks, or a schema change. This
 * slice does not resolve that; it only refuses to hide the gap by leaving the fields out of the
 * command.
 *
 * <p>Most of the remaining fields line up with {@code PlanRequest} one-for-one — {@code sex},
 * {@code ageYears}, {@code weightKg}, {@code heightCm}, {@code activityLevel}, {@code mainGoal},
 * {@code planObjective}, {@code trainingDaysPerWeek}, {@code trainingWeekdays}, {@code equipment},
 * {@code mealsPerDay}, {@code dietPattern}, {@code cuisineStyle}, {@code planKcal}, {@code
 * targetProteinG/CarbsG/FatG} — deliberately not constructed FROM a {@code PlanRequest} here (no
 * dependency from this record to that one) so this type stays usable in isolation and does not
 * pretend the {@code weeks}/{@code startDate} gap above is already solved.
 *
 * <p><b>A second, narrower disagreement.</b> {@code PlanRequest#trainingWeekdays} is a {@code
 * null}-or-{@code Set<DayOfWeek>} that distinguishes "nobody answered" ({@code null}) from
 * "answered: none" (empty set) — see its own javadoc. The contract's wire format has only one
 * representation for training days ({@code training.weekdays}, "lista vacía = no lo dijo"), which
 * collapses that distinction. {@link Training#weekdays} is therefore a plain, never-null {@code
 * Set<DayOfWeek>} (empty means "nobody said or said none" — the same information the wire carries),
 * and whoever maps a {@code PlanRequest} into this command (slice 6) is the one who has to decide
 * what to do with a {@code null} — this type cannot preserve a distinction the wire format does not
 * have.
 *
 * @param contractVersion which shape of this message is being spoken; {@code "1"} today (ADR-015
 *     decision 13)
 * @param planRequestId the {@code plan_request} row this asks on behalf of; echoed back unchanged
 *     by the agent so the response can be matched to the request that caused it
 * @param catalogVersion which snapshot of the food catalog travels in {@link #catalog}
 */
public record PlanGenerationCommand(
    String contractVersion,
    UUID planRequestId,
    String catalogVersion,
    Person person,
    Goal goal,
    Targets targets,
    Training training,
    Preferences preferences,
    PlanScope plan,
    List<PlanCatalogEntry> catalog) {

  public PlanGenerationCommand {
    if (contractVersion == null || contractVersion.isBlank()) {
      throw new IllegalArgumentException("contractVersion must not be blank");
    }
    Objects.requireNonNull(planRequestId, "planRequestId must not be null");
    if (catalogVersion == null || catalogVersion.isBlank()) {
      throw new IllegalArgumentException("catalogVersion must not be blank");
    }
    Objects.requireNonNull(person, "person must not be null");
    Objects.requireNonNull(goal, "goal must not be null");
    Objects.requireNonNull(targets, "targets must not be null");
    Objects.requireNonNull(training, "training must not be null");
    Objects.requireNonNull(preferences, "preferences must not be null");
    Objects.requireNonNull(plan, "plan must not be null");
    catalog = catalog == null ? List.of() : List.copyOf(catalog);
  }

  public record Person(
      Sex sex, int ageYears, double weightKg, double heightCm, ActivityLevel activityLevel) {}

  /**
   * Two objectives, not one — see {@code PlanRequest}'s own javadoc for why (ADR-015 decision 5).
   */
  public record Goal(MainGoal mainGoal, PlanObjective planObjective) {}

  /** {@code planKcal} is already computed and frozen; the agent must not recompute it. */
  public record Targets(int planKcal, Double proteinG, Double carbsG, Double fatG) {}

  public record Training(
      int daysPerWeek, Set<DayOfWeek> weekdays, Set<TrainingEquipment> equipment) {
    public Training {
      weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
      equipment = equipment == null ? Set.of() : Set.copyOf(equipment);
    }
  }

  public record Preferences(int mealsPerDay, DietPattern dietPattern, CuisineStyle cuisineStyle) {}

  /**
   * How many weeks to write, and where day 1 of week 1 falls. See the class javadoc's disagreement.
   */
  public record PlanScope(int weeks, LocalDate startDate) {}
}

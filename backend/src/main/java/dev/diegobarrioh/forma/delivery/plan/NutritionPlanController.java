package dev.diegobarrioh.forma.delivery.plan;

import dev.diegobarrioh.forma.application.CurrentUserProvider;
import dev.diegobarrioh.forma.application.NutritionPlan;
import dev.diegobarrioh.forma.application.NutritionPlanReader;
import dev.diegobarrioh.forma.application.NutritionPlanService;
import dev.diegobarrioh.forma.application.PlanActivationService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.PlanStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nutrition-plan REST endpoints (V53/V54): {@code /api/v1/nutrition/plans}.
 *
 * <p>NOT admin-only, unlike the food and recipe catalogues. Those are shared reference data that
 * one curator maintains for everybody; a plan is somebody's own diet. Every method here is scoped
 * to the authenticated caller, and a plan belonging to another account answers as absent rather
 * than as forbidden — the same answer an id that never existed gets, which tells a prober nothing.
 *
 * <p>Thin controller (ADR-001, ADR-005): {@link NutritionPlanService} owns the writes and the
 * one-active-plan rule, {@link NutritionPlanReader} works the numbers out, and nothing here does
 * either.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/nutrition/plans")
public class NutritionPlanController {

  private final NutritionPlanService service;
  private final NutritionPlanReader reader;
  private final CurrentUserProvider currentUserProvider;
  private final PlanActivationService activations;

  public NutritionPlanController(
      NutritionPlanService service,
      NutritionPlanReader reader,
      CurrentUserProvider currentUserProvider,
      PlanActivationService activations) {
    this.service = service;
    this.reader = reader;
    this.currentUserProvider = currentUserProvider;
    this.activations = activations;
  }

  /**
   * The caller's plans, newest first.
   *
   * <p>Headers only: a list showing four twelve-week plans would otherwise resolve some three
   * hundred lines against the catalog to render four names and four dates.
   */
  @GetMapping
  public List<NutritionPlanResponse> list() {
    return service.findAll(userId()).stream().map(NutritionPlanResponse::summary).toList();
  }

  /** One plan of the caller's, with its days worked out. */
  @GetMapping("/{id}")
  public NutritionPlanResponse find(@PathVariable UUID id) {
    UUID user = userId();
    NutritionPlan plan = service.findById(user, id);
    return NutritionPlanResponse.from(plan, reader.days(user, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NutritionPlanResponse create(@Valid @RequestBody NutritionPlanRequest request) {
    UUID user = userId();
    // Created as a draft whatever the body says. Activating is its own call because it has to stand
    // down whichever plan the user was following, and a create that silently did that would be a
    // surprising thing for a POST to do.
    NutritionPlan stored = service.create(request.toPlan(user, PlanStatus.DRAFT));
    return NutritionPlanResponse.from(stored, reader.days(user, stored.id()));
  }

  /**
   * Replaces the plan at {@code id}, leaving its status where it was.
   *
   * <p>The days, meals and lines are replaced whole: the editor shows the complete plan, so what it
   * leaves out is what somebody removed.
   */
  @PutMapping("/{id}")
  public NutritionPlanResponse update(
      @PathVariable UUID id, @Valid @RequestBody NutritionPlanRequest request) {
    UUID user = userId();
    NutritionPlan stored = service.update(user, id, request.toPlan(user, PlanStatus.DRAFT));
    return NutritionPlanResponse.from(stored, reader.days(user, id));
  }

  /**
   * Makes this the plan the caller is following; whatever they followed becomes COMPLETED.
   *
   * <p>Goes through {@link PlanActivationService} rather than straight to {@code service.activate}
   * so the acceptance is recorded too (V58). Flipping the status alone left the training calendar
   * empty for good: its gate asks whether this account is following a plan, and activating from
   * here never answered.
   */
  @PostMapping("/{id}/activation")
  public NutritionPlanResponse activate(@PathVariable UUID id) {
    UUID user = userId();
    return NutritionPlanResponse.from(activations.activate(user, id), reader.days(user, id));
  }

  /**
   * Moves a plan to DRAFT, COMPLETED or ARCHIVED.
   *
   * <p>Not ACTIVE: that is {@code POST /activation}, which has more to do than set a column, and
   * having one rule reachable through two doors is how the two end up disagreeing.
   */
  @PutMapping("/{id}/status")
  public NutritionPlanResponse changeStatus(
      @PathVariable UUID id, @RequestBody StatusRequest request) {
    UUID user = userId();
    return NutritionPlanResponse.from(
        service.changeStatus(user, id, parseStatus(request.status())), reader.days(user, id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(userId(), id);
  }

  /** The status to move to. */
  public record StatusRequest(String status) {}

  private UUID userId() {
    return currentUserProvider.currentUserId();
  }

  private static PlanStatus parseStatus(String status) {
    if (status == null) {
      throw new ValidationException("Falta el estado.");
    }
    try {
      return PlanStatus.valueOf(status.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw new ValidationException("Estado desconocido: " + status);
    }
  }
}

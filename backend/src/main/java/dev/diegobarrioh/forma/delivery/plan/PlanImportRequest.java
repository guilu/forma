package dev.diegobarrioh.forma.delivery.plan;

import jakarta.validation.Valid;
import java.util.List;

/**
 * A file of nutrition plans, for several accounts at once (import).
 *
 * <p>The plan itself is a {@link NutritionPlanRequest}, unchanged — the same body {@code POST
 * /nutrition/plans} already takes. That is deliberate and not a shortcut: a format for a model to
 * generate and a format for a form to submit are the same information, and having two would mean
 * every field added to one had to be remembered in the other.
 *
 * <p>What this adds is the only thing a file needs and a single request does not: which account
 * each plan is for. A plan is somebody's own diet, so importing one into an account that is not
 * yours is an administrator's act and the endpoint says so.
 *
 * <p>Plans arrive as DRAFT whatever they ask for. Somebody's diet changing because a file was
 * uploaded would be a surprising thing for an import to do; each account activates its own from the
 * plans screen.
 *
 * @param plans the file's contents; each entry names an account and carries one plan
 */
public record PlanImportRequest(@Valid List<Entry> plans) {

  /**
   * @param forUserEmail the account this plan is for, by email — the one thing about a person that
   *     whoever writes the file already knows. Account ids are ours and would have to be looked up
   *     first
   * @param plan the plan, in exactly the shape {@code POST /nutrition/plans} accepts
   */
  public record Entry(String forUserEmail, @Valid NutritionPlanRequest plan) {}
}

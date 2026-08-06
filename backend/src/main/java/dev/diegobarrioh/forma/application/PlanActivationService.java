package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PlanStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The door between "a plan was written for you" and "you are following it".
 *
 * <p>An account is seeded with its plan already drafted and switched OFF, so the first thing it
 * sees after logging in is a question rather than somebody else's week. Until that question is
 * answered, the training calendar and the nutrition day stay empty — not because the data is
 * missing, but because nobody has said to start.
 *
 * <p>Two different things are stored, and they are stored apart on purpose:
 *
 * <ul>
 *   <li>the PLAN's status, which is what {@code findActive} reads and what the nutrition screens
 *       already answer for themselves;
 *   <li>the ACCEPTANCE, which is what the TRAINING gate reads — the training plan lives in code
 *       ({@code RunningPlanGenerator}) and has no row whose status could be asked.
 * </ul>
 *
 * <p>That is why the nutrition endpoints need no gate of their own: a draft plan is invisible to
 * {@code findActive} by construction, so the day comes back empty without anybody checking a flag.
 * A gate there would be a second opinion about the same fact, free to disagree with the first.
 */
@Service
public class PlanActivationService {

  private final NutritionPlanService plans;
  private final PlanAcceptanceRepository acceptances;
  private final CurrentUserProvider currentUserProvider;

  public PlanActivationService(
      NutritionPlanService plans,
      PlanAcceptanceRepository acceptances,
      CurrentUserProvider currentUserProvider) {
    this.plans = plans;
    this.acceptances = acceptances;
    this.currentUserProvider = currentUserProvider;
  }

  /** Whether this account has said yes to its plan. What the training gate asks. */
  public boolean accepted() {
    return acceptances.accepted(currentUserProvider.currentUserId());
  }

  /**
   * The plan waiting to be started, if there is one.
   *
   * <p>Nothing waiting is an ordinary answer, not a fault: an account whose plan generation failed
   * has no draft, and is told so on the screens rather than asked to accept something absent.
   */
  public PlanAcceptance pending() {
    UUID userId = currentUserProvider.currentUserId();
    if (acceptances.accepted(userId)) {
      return PlanAcceptance.nothing();
    }
    return draftOf(userId)
        .map(plan -> PlanAcceptance.of(plan.name()))
        .orElseGet(PlanAcceptance::nothing);
  }

  /**
   * Starts the plan: switches it on and records that the account asked for it.
   *
   * <p>Both or neither. An activation without the acceptance would leave the training screen empty
   * beside a nutrition screen full of food; an acceptance without the activation would do the
   * reverse. They are one decision, so they are one transaction.
   *
   * @throws NotFoundException if there is no plan waiting to be started
   */
  @Transactional
  public void accept() {
    UUID userId = currentUserProvider.currentUserId();
    NutritionPlan draft =
        draftOf(userId)
            .orElseThrow(() -> new NotFoundException("No hay ningún plan pendiente de activar."));
    plans.activate(userId, draft.id());
    acceptances.markAccepted(userId, Instant.now());
  }

  /**
   * The plan written but never started.
   *
   * <p>Only a {@link PlanStatus#DRAFT} counts. A COMPLETED plan is history and an ARCHIVED one was
   * put away deliberately; offering either as "your plan" would be offering to redo a past week.
   */
  private Optional<NutritionPlan> draftOf(UUID userId) {
    return plans.findAll(userId).stream()
        .filter(plan -> plan.status() == PlanStatus.DRAFT)
        .findFirst();
  }
}

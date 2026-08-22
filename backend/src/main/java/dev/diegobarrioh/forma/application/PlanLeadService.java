package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.EnergyRequirement;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * What happens when somebody finishes the public funnel.
 *
 * <p>Lives here and not in the controller (ADR-005): working out the requirement and keeping the
 * request are the behaviour, and a controller that does both is a controller with business rules in
 * it.
 */
@Service
public class PlanLeadService {

  /**
   * Which privacy notice a lead accepted.
   *
   * <p>A date, not a number, because that is what the page itself shows as «última actualización»
   * and the two have to agree — a stored version nobody can look up proves nothing. <b>Bump this
   * whenever the notice changes materially</b>, and only then: a typo fix is not a new version, a
   * new purpose or a new recipient is.
   *
   * <p>Server-side rather than sent by the client, deliberately. The browser must not be the one
   * saying which text it agreed to: it is the side that can lie about it, and the notice in force
   * at a given instant is a fact the server owns.
   */
  public static final String PRIVACY_POLICY_VERSION = "2026-08-22";

  private final PlanLeadRepository repository;
  private final Clock clock;

  public PlanLeadService(PlanLeadRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Keeps a finished funnel and returns the requirement it was worked out with.
   *
   * <p>The requirement is computed here, once, and both stored and returned: the number the screen
   * shows and the number kept as the request have to be the same number, and computing it twice is
   * how they stop being.
   */
  public EnergyRequirement record(PlanDraft draft) {
    EnergyRequirement requirement =
        EnergyRequirement.of(
            draft.sex(),
            draft.ageYears(),
            draft.weightKg(),
            draft.heightCm(),
            draft.activityLevel(),
            draft.objective());

    Instant now = clock.instant();
    repository.save(
        new PlanLead(
            UUID.randomUUID(),
            draft.fullName(),
            draft.email(),
            draft.country(),
            draft.heardAboutUs(),
            draft.sex(),
            draft.ageYears(),
            draft.weightKg(),
            draft.heightCm(),
            draft.activityLevel(),
            draft.objective(),
            draft.daysPerWeek(),
            draft.mealsPerDay(),
            draft.eatingStyle(),
            requirement.planKcal(),
            PRIVACY_POLICY_VERSION,
            draft.wantsMarketing()),
        now);

    return requirement;
  }
}

package dev.diegobarrioh.forma.delivery.generator;

import dev.diegobarrioh.forma.application.PlanDraft;
import dev.diegobarrioh.forma.application.PlanLeadService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.EnergyRequirement;
import dev.diegobarrioh.forma.domain.PlanObjective;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public plan generator: {@code /api/v1/public/plan-generator}.
 *
 * <p>UNAUTHENTICATED, and the only part of the API that is besides logging in. Somebody who has
 * never heard of FORMA answers four screens and gets a plan; asking them to register first would
 * make the funnel pointless. Everything here is therefore rate-limited by nothing yet — see the
 * note on {@link #generate}, which since V61 also WRITES.
 *
 * <p>{@code /energy-requirement} exists so the funnel can show its arithmetic without owning it.
 * The frontend says of itself that it "owns no nutrition rules and never recomputes macros", and
 * Mifflin-St Jeor is a nutrition rule: written in React as well, it would be free to drift from the
 * copy the generator actually builds plans with, and the number somebody trusted would stop being
 * the number they got.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/public/plan-generator")
public class PlanGeneratorController {

  private final PlanLeadService leads;

  public PlanGeneratorController(PlanLeadService leads) {
    this.leads = leads;
  }

  /**
   * The daily requirement, worked out.
   *
   * <p>Called as the funnel's first two steps are filled in. Nothing is stored: it is a
   * calculation, and a calculation with no side effect can be asked for as often as anybody likes.
   */
  @PostMapping("/energy-requirement")
  public EnergyRequirementResponse energyRequirement(
      @Valid @RequestBody EnergyRequirementRequest request) {
    // No objective yet on step 1: the screen shows basal and daily, and the adjustment arrives with
    // step 2. MAINTENANCE multiplies by one, so the absent case and the neutral case agree.
    PlanObjective objective =
        request.objective() == null ? PlanObjective.MAINTENANCE : request.objective();
    return EnergyRequirementResponse.from(
        EnergyRequirement.of(
            request.sex(),
            request.ageYears(),
            request.weightKg(),
            request.heightCm(),
            request.activityLevel(),
            objective));
  }

  /**
   * Accepts a finished funnel and keeps it.
   *
   * <p>V61 ended the part of this that was indefensible. Until then the endpoint validated what it
   * was given and said yes, which meant the funnel DROPPED EVERY LEAD: somebody answered four
   * screens, gave their email, saw a success page, and no trace of them survived the request.
   *
   * <p><b>Still not generated, still not sent.</b> The plan, the PDF and the mail do not exist yet,
   * and {@link dev.diegobarrioh.forma.application.PlanLead} is what makes building them later
   * possible for the people who asked first. The success screen must not promise a delivery until
   * they do.
   *
   * <p>Nor is there any rate limiting. An unauthenticated endpoint that now WRITES a row is a
   * better spam target than one that wrote nothing, and the protection has to exist before the mail
   * does — it is the next thing here.
   */
  @PostMapping
  public PlanDraftAccepted generate(@Valid @RequestBody PlanDraftRequest request) {
    EnergyRequirement requirement =
        leads.record(
            new PlanDraft(
                request.fullName(),
                request.email(),
                request.country(),
                request.heardAboutUs(),
                request.sex(),
                request.ageYears(),
                request.weightKg(),
                request.heightCm(),
                request.activityLevel(),
                request.objective(),
                request.daysPerWeek(),
                request.mealsPerDay(),
                request.eatingStyle(),
                request.wantsMarketing()));
    return new PlanDraftAccepted(request.email(), requirement.planKcal(), request.mealsPerDay());
  }
}

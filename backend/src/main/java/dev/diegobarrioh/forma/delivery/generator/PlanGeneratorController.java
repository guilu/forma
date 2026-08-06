package dev.diegobarrioh.forma.delivery.generator;

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
 * note on {@link #generate}.
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
   * Accepts a finished funnel.
   *
   * <p><b>NOTHING IS GENERATED AND NOTHING IS STORED YET.</b> This endpoint exists so the four
   * screens can be built and used end to end while the three pieces behind it do not exist: the
   * plan generator, the PDF, and sending mail. It validates what it is given and says yes.
   *
   * <p>Which means the funnel currently DROPS EVERY LEAD. Somebody fills in four screens, gives
   * their email and gets a success page, and no trace of them survives the request. That is
   * acceptable while this is being built and is not acceptable the day it is put in front of
   * anybody — it is the first thing to fix, ahead of the plan itself.
   *
   * <p>Nor is there any rate limiting. An unauthenticated endpoint that will eventually create
   * accounts and send mail is a spam vector, and the protection has to exist before the mail does.
   */
  @PostMapping
  public PlanDraftAccepted generate(@Valid @RequestBody PlanDraftRequest request) {
    EnergyRequirement requirement =
        EnergyRequirement.of(
            request.sex(),
            request.ageYears(),
            request.weightKg(),
            request.heightCm(),
            request.activityLevel(),
            request.objective());
    return new PlanDraftAccepted(request.email(), requirement.planKcal(), request.mealsPerDay());
  }
}

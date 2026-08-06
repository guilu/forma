package dev.diegobarrioh.forma.delivery.generator;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A finished funnel: everything the four screens collected.
 *
 * <p>What is NOT here is as deliberate as what is. No pathologies, no dietary restrictions, no
 * allergies — those sit behind the padlock in the funnel, shown as what a subscription unlocks and
 * never asked for. They are health data, and collecting a special category of personal data on a
 * public form to then do nothing with it is the worst of both options.
 *
 * @param email where the plan is sent. The only field that identifies anybody
 * @param acceptsPrivacyPolicy required, and required to be true: consent that can be false is not
 *     consent, and a checkbox nobody has to tick is decoration
 * @param wantsMarketing whether they also want tips and news, which is a separate question from
 *     agreeing to be sent the thing they asked for
 */
public record PlanDraftRequest(
    @NotNull Sex sex,
    @NotNull @Min(14) @Max(120) Integer ageYears,
    @NotNull @Positive @Max(400) Double weightKg,
    @NotNull @Positive @Max(260) Double heightCm,
    @NotNull ActivityLevel activityLevel,
    @NotNull PlanObjective objective,
    @NotNull @Min(3) @Max(7) Integer daysPerWeek,
    @NotNull @Min(3) @Max(6) Integer mealsPerDay,
    @NotBlank @Size(max = 64) String eatingStyle,
    @NotBlank @Size(max = 120) String fullName,
    @NotNull @Email @Size(max = 320) String email,
    @Size(max = 64) String country,
    @Size(max = 64) String heardAboutUs,
    boolean wantsMarketing,
    @AssertTrue(message = "Hay que aceptar el aviso de privacidad") boolean acceptsPrivacyPolicy) {}

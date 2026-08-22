package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;

/**
 * A finished funnel, on its way in.
 *
 * <p>Separate from the delivery layer's {@code PlanDraftRequest} so the application does not depend
 * on a web request shape — the same funnel could arrive from somewhere that is not an HTTP POST,
 * and the validation annotations that belong on the edge have no business in here.
 *
 * <p>{@code acceptsPrivacyPolicy} is absent on purpose. The edge already refuses anything but true,
 * and carrying it inwards would invite a second check that could disagree with the first: the only
 * drafts that exist are consented ones.
 */
public record PlanDraft(
    String fullName,
    String email,
    String country,
    String heardAboutUs,
    Sex sex,
    int ageYears,
    double weightKg,
    double heightCm,
    ActivityLevel activityLevel,
    PlanObjective objective,
    int daysPerWeek,
    int mealsPerDay,
    String eatingStyle,
    boolean wantsMarketing) {}

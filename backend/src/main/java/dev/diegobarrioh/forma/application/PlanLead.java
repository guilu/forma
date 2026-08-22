package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;
import java.util.UUID;

/**
 * Somebody who finished the public funnel and asked for a plan.
 *
 * <p>Not a {@code User}: no password, no way in, and most of them will never register. What it is
 * is a request — everything needed to build the plan that was promised, plus the proof that its
 * owner agreed to be sent it.
 *
 * <p>{@code planKcal} is stored rather than recomputed on read. It is the figure this person was
 * shown and acted on; if the formula moves, the plan they eventually receive has to be comparable
 * against the number that convinced them, not against the one today's arithmetic would give.
 *
 * @param privacyPolicyVersion which notice was in force when they accepted. Article 7.1 asks for
 *     proof of consent, and proof without the text it applied to is not proof
 */
public record PlanLead(
    UUID id,
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
    int planKcal,
    String privacyPolicyVersion,
    boolean wantsMarketing) {}

package dev.diegobarrioh.forma.delivery.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Request body for {@code PATCH /api/v1/profile} (FOR-107): the "Profile fields" section (name,
 * email, birthDate, sex, heightCm, activityLevel, mainGoal).
 *
 * <p>Every field is optional and partial — a {@code null}/omitted field leaves the stored value
 * unchanged (spec FOR-107 Edge Cases: "Partial update... must not clobber unrelated preference
 * fields"); the merge itself happens in {@link
 * dev.diegobarrioh.forma.application.UserProfileService}, not here. {@code sex}, {@code
 * activityLevel} and {@code mainGoal} are validated as {@code String}s against the known enum names
 * here (not the enum types) so an unknown value yields {@code VALIDATION_ERROR} instead of a
 * Jackson enum-parse failure surfacing as 500, mirroring {@code ShoppingProductRequest.category}
 * (FOR-106).
 *
 * @param name display name
 * @param email contact email
 * @param birthDate date of birth (ISO-8601)
 * @param sex one of {@code MALE}, {@code FEMALE}, {@code OTHER}
 * @param heightCm height in centimeters; strictly positive when present
 * @param activityLevel one of {@code SEDENTARY}, {@code LIGHT}, {@code MODERATE}, {@code ACTIVE},
 *     {@code VERY_ACTIVE}
 * @param mainGoal one of {@code COMPOSICION}, {@code RENDIMIENTO}, {@code HABITO}
 */
public record UpdateProfileFieldsRequest(
    String name,
    String email,
    LocalDate birthDate,
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "must be one of MALE, FEMALE, OTHER")
        String sex,
    @Positive Double heightCm,
    @Pattern(
            regexp = "SEDENTARY|LIGHT|MODERATE|ACTIVE|VERY_ACTIVE",
            message = "must be one of SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE")
        String activityLevel,
    @Pattern(
            regexp = "COMPOSICION|RENDIMIENTO|HABITO",
            message = "must be one of COMPOSICION, RENDIMIENTO, HABITO")
        String mainGoal) {}

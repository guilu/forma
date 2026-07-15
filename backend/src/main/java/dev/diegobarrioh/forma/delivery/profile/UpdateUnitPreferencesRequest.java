package dev.diegobarrioh.forma.delivery.profile;

import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PATCH /api/v1/profile/units} (FOR-107): weight/height/distance/energy
 * unit preferences.
 *
 * <p>Every field is optional and partial (same merge-not-clobber contract as {@link
 * UpdateProfileFieldsRequest}). Each field is validated as a {@code String} against the single
 * MVP-supported value for that dimension (spec FOR-107: "MVP ships the metric set... as the default
 * and only supported value") so an unsupported value yields {@code VALIDATION_ERROR} rather than a
 * 500, while still being a real per-dimension preference FOR-119 can extend later.
 *
 * @param weightUnit must be {@code KG} when present
 * @param heightUnit must be {@code CM} when present
 * @param distanceUnit must be {@code KM} when present
 * @param energyUnit must be {@code KCAL} when present
 */
public record UpdateUnitPreferencesRequest(
    @Pattern(regexp = "KG", message = "must be one of KG") String weightUnit,
    @Pattern(regexp = "CM", message = "must be one of CM") String heightUnit,
    @Pattern(regexp = "KM", message = "must be one of KM") String distanceUnit,
    @Pattern(regexp = "KCAL", message = "must be one of KCAL") String energyUnit) {}

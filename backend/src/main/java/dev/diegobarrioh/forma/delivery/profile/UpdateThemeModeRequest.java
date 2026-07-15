package dev.diegobarrioh.forma.delivery.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PATCH /api/v1/profile/theme} (FOR-107): the theme preference.
 *
 * <p>Unlike the other profile PATCH endpoints, there is only one field, so it is required rather
 * than partial — {@code themeMode} must always be supplied. Validated as a {@code String} against
 * the known {@link dev.diegobarrioh.forma.domain.ThemeMode} names (mirroring the frontend's {@code
 * ThemeMode} vocabulary, FOR-62) so an unknown value yields {@code VALIDATION_ERROR}.
 *
 * @param themeMode one of {@code LIGHT}, {@code DARK}, {@code SYSTEM}; required
 */
public record UpdateThemeModeRequest(
    @NotNull @Pattern(regexp = "LIGHT|DARK|SYSTEM", message = "must be one of LIGHT, DARK, SYSTEM")
        String themeMode) {}

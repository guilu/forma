package dev.diegobarrioh.forma.delivery.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body accepted when renaming or re-drawing a category (FOR-197, admin only).
 *
 * <p>Carries no code and no scope: both are in the path, and both are what the row IS. Accepting
 * them in the body would offer a rename the endpoint has to ignore.
 *
 * <p>The icon is capped at a few characters because it is a glyph, not a caption — enough for an
 * emoji, including the multi-codepoint ones, and not enough for a sentence that would break every
 * table it lands in.
 */
public record CategoryDisplayRequest(
    @NotBlank @Size(max = 64) String label, @Size(max = 16) String icon) {}

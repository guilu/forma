package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.CategoryScope;

/**
 * How one category is written and drawn (FOR-197).
 *
 * <p>Presentation, not identity. {@code code} is the value stored on every row and checked by the
 * database; {@code label} and {@code icon} are what a person sees, and are the only two things an
 * admin can change. Renaming "Lácteo" does not move a single row.
 *
 * @param scope which vocabulary this belongs to
 * @param code the stored token — never editable, since rows point at it
 * @param label what a person reads
 * @param icon a glyph, or absent: decoration is allowed to be missing, a name is not
 */
public record CategoryDisplay(CategoryScope scope, String code, String label, String icon) {

  public CategoryDisplay {
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("label must not be blank");
    }
  }
}
